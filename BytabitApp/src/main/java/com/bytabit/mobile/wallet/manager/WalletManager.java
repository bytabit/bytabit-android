package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.ErrorResult;
import com.bytabit.mobile.common.Event;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.common.Result;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.nav.evt.QuitEvent;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.*;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bytabit.mobile.trade.TradeManager.TRADES_PATH;
import static org.bitcoinj.core.Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;

public class WalletManager {

    private final static String TRADE_WALLET_FILE_NAME = "trade.wallet";
    private final static String ESCROW_WALLET_FILE_NAME = "escrow.wallet";
    private final static String BACKUP_EXT = ".bkp";
    private final static String SAVE_EXT = ".sav";

    //private final static Logger LOG = LoggerFactory.getLogger(WalletManager.class);

    private final EventLogger eventLogger = EventLogger.of(WalletManager.class);

    private final NetworkParameters netParams;

    private final Context btcContext;

    // UI bindable properties
//    private final Observable<Double> downloadProgress;
//    private final Observable<TransactionWithAmt> tradeWalletTransactions;
//    private final BooleanProperty started;

    // rxJava observables
//    private final Observable<Wallet> tradeWalletObservable;

    private final PublishSubject<WalletAction> actions;

    private final Observable<WalletResult> walletResults;

    private final ConnectableObservable<BlockDownloadResult> blockDownloadResults;

    private final ConnectableObservable<TransactionResult> tradeWalletTransactionResults;

//    @Getter
//    private final Observable<TransactionResult> escrowWalletTransactionResults;

    ////

    private final Single<Wallet> tradeWallet;
//    private final Observable<String> tradeWalletBalance;

//    private final PublishSubject<EscrowWalletAction> escrowWalletActions;

//    private final Observable<TransactionResult> escrowWalletTransactionResults;

    //    private final PublishSubject<Wallet> createdEscrowWallets;
    //private final PublishSubject<String> createdEscrowAddresses;
    //    private final Observable<Wallet> escrowWallets;
    //private final Observable<TransactionUpdatedEvent> escrowTxUpdatedEvents;

//    private Disposable blockDownloadSubscription;

    public WalletManager() {

        netParams = NetworkParameters.fromID("org.bitcoin." + AppConfig.getBtcNetwork());

        btcContext = Context.getOrCreate(netParams);

        actions = PublishSubject.create();

        Observable<WalletAction> actionObservable = actions
                .map(a -> a)
                .compose(eventLogger.logEvents()).share();

        // create trade wallet

        tradeWallet = Single.<Wallet>create(source -> {
            try {
                source.onSuccess(createOrLoadTradeWallet());
            } catch (FileNotFoundException | UnreadableWalletException ex) {
                source.onError(ex);
            }
        }).cache();

        // wallet actions to results

        Observable<WalletResult> tradeWalletInfoResults = actionObservable.ofType(GetTradeWalletInfo.class)
                .flatMap(e -> tradeWallet.map(tw -> {
                            Context.propagate(btcContext);
                            return new TradeWalletInfo(getSeedWords(tw), getXpubKey(tw), getXprvKey(tw));
                        }
                ).toObservable());

        Observable<WalletResult> tradeWalletProfilePubKeyResults = actionObservable
                .ofType(GetProfilePubKey.class)
                .flatMap(a -> tradeWallet
                        .map(this::getFreshBase58AuthPubKey)
                        .map(ProfilePubKey::new).toObservable());

        Observable<WalletResult> tradeWalletEscrowPubKeyResults = actionObservable
                .ofType(GetEscrowPubKey.class)
                .flatMap(a -> tradeWallet
                        .map(this::getFreshBase58ReceivePubKey)
                        .map(EscrowPubKey::new).toObservable());

        Observable<WalletResult> tradeWalletDepositAddressResults = actionObservable
                .ofType(GetTradeWalletDepositAddress.class)
                .flatMap(a -> tradeWallet
                        .map(this::getDepositAddress)
                        .map(TradeWalletDepositAddress::new).toObservable());

        walletResults = tradeWalletInfoResults
                .mergeWith(tradeWalletProfilePubKeyResults)
                .mergeWith(tradeWalletEscrowPubKeyResults)
                .mergeWith(tradeWalletDepositAddressResults)
                .compose(eventLogger.logResults())
                .subscribeOn(Schedulers.io()).share();

        // trade wallet tx updated  events

        Observable<Transaction> loadTradeWalletTx = tradeWallet.map(tw -> tw.getTransactions(false))
                .flattenAsObservable(txs -> txs);

        Observable<TransactionResult> loadTradeWalletTxResults = tradeWallet.flatMapObservable(tw ->
                loadTradeWalletTx.map(tx -> {
                    Context.propagate(btcContext);
                    TransactionWithAmt txWithAmt = TransactionWithAmt.builder()
                            .tx(tx)
                            .transactionAmt(tx.getValue(tw))
                            .outputAddress(getWatchedOutputAddress(tx, tw))
                            .inputTxHash(tx.getInput(0).getOutpoint().getHash().toString())
                            .walletBalance(tw.getBalance())
                            .build();
                    return new TradeWalletUpdate(txWithAmt);
                }));

        Observable<TransactionResult> changeTradeWalletResults = tradeWallet.observeOn(Schedulers.io())
                .flatMapObservable(tw -> Observable.<TradeWalletUpdate>create(source -> {
                            TransactionConfidenceEventListener listener = new TransactionConfidenceEventListener() {

                                @Override
                                public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                                    Context.propagate(btcContext);
                                    TransactionWithAmt txWithAmt = TransactionWithAmt.builder()
                                            .tx(tx)
                                            .transactionAmt(tx.getValue(wallet))
                                            .outputAddress(getWatchedOutputAddress(tx, wallet))
                                            .inputTxHash(tx.getInput(0).getOutpoint().getHash().toString())
                                            .walletBalance(tw.getBalance())
                                            .build();
                                    source.onNext(new TradeWalletUpdate(txWithAmt));
                                }
                            };
                            tw.addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);

                            source.setCancellable(() -> {
                                tw.removeTransactionConfidenceEventListener(listener);
                            });
                        }).groupBy(u -> u.transactionWithAmt.getHash())
                                .flatMap(ug -> ug.throttleLast(1, TimeUnit.SECONDS))
                );

        tradeWalletTransactionResults = loadTradeWalletTxResults
                .concatWith(changeTradeWalletResults)
                .compose(eventLogger.logResults())
                .subscribeOn(Schedulers.io())
                .replay(15, TimeUnit.MINUTES);

        // create escrow escrowWallets

        Single<File> tradesDir = Single.just(new File(TRADES_PATH));

//        Single<List<Wallet>> readEscrowWallets = tradesDir
//                .map(td -> Arrays.asList(td != null && td.list() != null ? td.list() : new String[0]))
//                .flattenAsObservable(escrowAddresses -> escrowAddresses)
//                .collectInto(new ArrayList<>(), (ewl, escrowAddress) -> {
//                    File escrowWalletFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME);
//                    File escrowWalletBackupFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME + BACKUP_EXT);
//                    ewl.add(createOrLoadWallet(escrowWalletFile, escrowWalletBackupFile));
//                });

        Observable<String> readEscrowAddresses = tradesDir
                .map(td -> Arrays.asList(td != null && td.list() != null ? td.list() : new String[0]))
                .flattenAsObservable(escrowAddresses -> escrowAddresses);

        Single<List<Wallet>> readEscrowWallets = readEscrowAddresses
                .map(this::createEscrowWallet).toList().cache();

//        createdEscrowWallets = PublishSubject.create();
//        createdEscrowAddresses = PublishSubject.create();

//        escrowWalletActions = PublishSubject.create();
//        Observable<Map<String, Wallet>> escrowWallets = escrowWalletActions.compose(eventLogger.logEvents()).collect(() -> (Map<String, Wallet>) new HashMap<String, Wallet>(), (wallets, action) -> {
//            switch (action.getType()) {
//                case START_ALL:
//                    readEscrowAddresses.forEach(a -> {
//                        wallets.put(a, createEscrowWallet(a));
//                    });
//                    break;
//                case ADD_ESCROW:
//                    String a = action.getEscrowAddress();
//                    wallets.put(a, createEscrowWallet(a));
//                    break;
//                case REMOVE_ESCROW:
//                    wallets.remove(action.getEscrowAddress());
//                    break;
//                default:
//                    throw new RuntimeException(String.format("Unexpected WalletAction.Type: %s", action.getType()));
//            }
//        }).toObservable();

//        Observable<Wallet> escrowWallets = readEscrowWallets.flattenAsObservable(rew -> rew)
//                .concatWith(createdEscrowAddresses.distinct().map(this::createEscrowWallet))
//                .publish().autoConnect();

//        Single<List<Wallet>> readEscrowWallets = readEscrowWalletEntries.flattenAsObservable(ewe -> ewe)
//                .map(Map.Entry::getValue).toList();

        // create blockchain
//        Single<BlockChain> blockChain = Single.zip(readEscrowWallets, blockStore, (rew, bs) ->
//                new BlockChain(netParams, rew, bs));

//        escrowWalletTransactionResults = //TBD

        // create block store

        Single<File> blockStoreFile = Single.just(new File(AppConfig.getPrivateStorage(), "bytabit.spvchain"));

        Single<BlockStore> blockStore = blockStoreFile.map(bsf -> {
            BlockStore bs = new SPVBlockStore(netParams, bsf);
            bs.getChainHead(); // detect corruptions as early as possible
            return bs;
        }).doOnError(t -> {
            blockStoreFile.subscribe(File::delete);
        }).retry(1).cache();

        // create block chain

        Single<BlockChain> blockChain = Single.zip(blockStore, tradeWallet, readEscrowWallets, (bs, tw, rew) -> {
            Context.propagate(btcContext);
            List<Wallet> wallets = new ArrayList<>();
            wallets.add(tw);
            wallets.addAll(rew);
            return new BlockChain(netParams, wallets, bs);
        }).cache();

        // create peer group
        // on error delete and re-create blockstore, reset escrowWallets?

        Single<PeerGroup> peerGroup = Single.zip(blockChain, tradeWallet, readEscrowWallets, (bc, tw, rew) -> {
            Context.propagate(btcContext);
            PeerGroup pg = createPeerGroup(bc, tw, rew);
            pg.start();
            return pg;
        }).cache();

        // post observable download events
        blockDownloadResults = peerGroup.observeOn(Schedulers.io()).flatMapObservable(pg ->
                Observable.<BlockDownloadResult>create(source -> {

                    pg.startBlockChainDownload(new DownloadProgressTracker() {
                        @Override
                        public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {
                            super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft);
                        }

                        @Override
                        protected void progress(double pct, int blocksSoFar, Date date) {
                            super.progress(pct, blocksSoFar, date);
                            source.onNext(new BlockDownloadUpdate(pct / 100.00));
                        }

                        @Override
                        protected void doneDownload() {
                            super.doneDownload();
                            source.onNext(new BlockDownloadDone());
                        }
                    });

                    // on un-subscribe stop peer group
                    source.setCancellable(pg::stop);
                })).subscribeOn(Schedulers.io())
                .compose(eventLogger.logResults())
                .throttleLast(1, TimeUnit.SECONDS)
                .replay(5);

        // escrow wallet tx updated events

//        escrowTxUpdatedEvents = Observable.zip(peerGroup.toObservable(), blockChain.toObservable(), escrowWallets, (pg, bc, ew) ->
//
//                Observable.<TransactionUpdatedEvent>create(source -> {
//                    try {
//                        pg.addWallet(ew);
//                        bc.addWallet(ew);
//                    } catch (IllegalStateException ex) {
//                        LOG.info(String.format("Can't add existing wallet: %s", ew.toString(false, false, false, null)));
//                    }
//                    TransactionConfidenceEventListener listener = new TransactionConfidenceEventListener() {
//
//                        @Override
//                        public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
//                            Context.propagate(btcContext);
//                            source.onNext(new TransactionUpdatedEventBuilder().tx(tx).wallet(wallet).build());
//                        }
//                    };
//                    ew.addTransactionConfidenceEventListener(listener);
//                })).flatMap(tue -> tue).share();

//        tradeWalletBalance = tradeWallet.toObservable().flatMap(tw -> tradeWalletTransactionResults
//                .map(te -> tw.getBalance().toFriendlyString())).share();

//        escrowWalletEntries = new HashMap<>();
//        escrowTxUpdatedEventEntries = new HashMap<>();

//
//        List<String> escrowAddresses;
//        if (tradesDir.list() != null) {
//            escrowAddresses = Arrays.asList(tradesDir.list());
//        } else {
//            escrowAddresses = new ArrayList<>();
//        }
//
//        for (String escrowAddress : escrowAddresses) {
//            final Single<File> escrowWalletFile = Single.create(source ->
//                    source.onSuccess(new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME))
//            );
//
//            final Single<File> escrowWalletBackupFile = Single.create(source ->
//                    source.onSuccess(new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME + BACKUP_EXT))
//            );
//
//            escrowWalletFile.zipWith(escrowWalletBackupFile, (ewf, ewbf) -> {
//                if (ewf.exists()) {
//                    escrowWalletEntries.put(escrowAddress, Single.create(source -> source.onSuccess(createOrLoadWallet(escrowWalletFile, escrowWalletBackupFile, blockStoreFile, blockStoreFileExists))));
//                }
//            });
//
//
//        }
//
//        // post observable wallet running events
//        for (String escrowAddress : escrowWalletEntries.keySet()) {
//            escrowTxUpdatedEventEntries.put(escrowAddress, Observable.<TransactionUpdatedEvent>create(source -> {
//                TransactionConfidenceEventListener listener = new TransactionConfidenceEventListener() {
//
//                    @Override
//                    public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
//                        Context.propagate(btcContext);
//                        source.onNext(new TransactionUpdatedEventBuilder().tx(tx).wallet(wallet).build());
//                    }
//                };
//                escrowWalletEntries.get(escrowAddress).addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);
//
//                source.setCancellable(() -> escrowWalletEntries.get(escrowAddress).removeTransactionConfidenceEventListener(listener));
//            }).share());
//        }


//        results = Observable.concat(loadTradeWalletTxResults, changeTradeWalletResults)
//                .mergeWith(blockDownloadResults)
//                .mergeWith(tradeWalletInfoResults)
//                .mergeWith(tradeWalletProfilePubKey)
//                .mergeWith(tradeWalletDepositAddress)
//                .compose(eventLogger.logResults())
//                .subscribeOn(Schedulers.io()).share();


        BytabitMobile.getNavEvents().distinctUntilChanged()
                .filter(ne -> ne instanceof QuitEvent).subscribe(qe -> {
            //LOG.debug("Got quit event");
            Context.propagate(btcContext);
            tradeWallet.subscribe(Wallet::shutdownAutosaveAndWait);
//            escrowWallets.map(Map::values).flatMapIterable(w -> w)
//                    .subscribe(Wallet::shutdownAutosaveAndWait);
            //LOG.debug("Shutdown escrowWallets autosave.");
        });
    }

//    public void start() {

//        if (!started.get()) {
//        LOG.debug("Wallet starting.");

//            started.setValue(true);

    // if blockstore file removed, reset escrowWallets
//        if (!blockStoreFile.exists()) {
//            LOG.debug("No blockstore file, resetting escrowWallets.");
//            tradeWallet.reset();
//            for (Wallet escrowWallet : escrowWalletEntries.values()) {
//                escrowWallet.reset();
//            }
//        }

    // get existing tx
//            Set<TransactionWithAmt> txsWithAmt = new HashSet<>();
//        Observable<TransactionWithAmt> loadedTradeTx = Observable.create(source -> {
//            for (Transaction t : tradeWallet.getTransactions(false)) {
//                source.onNext(new TransactionWithAmtBuilder().tx(t)
//                        .transactionAmt(t.getValue(tradeWallet))
//                        .outputAddress(getWatchedOutputAddress(t))
//                        .inputTxHash(t.getInput(0).getOutpoint().getHash().toString())
//                        .build());
//            }
//        });


//            javafx.application.Platform.runLater(() -> {
//                tradeWalletTransactions.addAll(txsWithAmt);
//                tradeWalletBalance.setValue(tradeWallet.getBalance().toFriendlyString());
//            });

    // listen for other events

//            blockDownloadSubscription = blockDownloadResults.subscribeOn(Schedulers.io())
//                    .subscribe(e -> {
//                        javafx.application.Platform.runLater(() -> {
//                            if (e instanceof BlockDownloadDone) {
//                                BlockDownloadDone dde = BlockDownloadDone.class.cast(e);
//                                downloadProgress.setValue(1.0);
//                            } else if (e instanceof BlockDownloadProgress) {
//                                BlockDownloadProgress dpe = BlockDownloadProgress.class.cast(e);
//                                downloadProgress.setValue(dpe.getPct());
//                            }
//                            tradeWalletBalance.setValue(getWalletBalance().toFriendlyString());
//                        });
//                    });

//            tradeWalletTransactionResults.observeOn(Schedulers.io())
//                    .subscribe(e -> {
//                        //LOG.debug("trade transaction updated event : {}", e);
//                        TransactionUpdatedEvent txe = TransactionUpdatedEvent.class.cast(e);
//
//                        TransactionWithAmt txu = new TransactionWithAmtBuilder().tx(txe.getTx()).transactionAmt(txe.getAmt()).outputAddress(getWatchedOutputAddress(txe.getTx())).inputTxHash(txe.getTx().getInput(0).getOutpoint().getHash().toString()).build();
//
//                        javafx.application.Platform.runLater(() -> {
//                            Integer index = tradeWalletTransactions.indexOf(txu);
//                            if (index > -1) {
//                                tradeWalletTransactions.set(index, txu);
//                            } else {
//                                tradeWalletTransactions.add(txu);
//                            }
//                            tradeWalletBalance.setValue(getWalletBalance().toFriendlyString());
//                        });
//                    });

//            for (String escrowAddress : escrowTxUpdatedEventEntries.keySet()) {
//                escrowTxUpdatedEventEntries.get(escrowAddress).observeOn(Schedulers.io())
//                        .subscribe(e -> {
//                            LOG.debug("escrow {} transaction updated event : {}", escrowAddress, e);
//                        });
//            }
//        LOG.debug("Wallet started.");
//        }
//    }

    private PeerGroup createPeerGroup(BlockChain blockChain, Wallet tradeWallet, List<Wallet> readEscrowWallets) {
        try {
//            BlockStore blockStore = new SPVBlockStore(netParams, blockStoreFile);
//            blockStore.getChainHead(); // detect corruptions as early as possible

//            if (!blockStoreFileExists && earliestKeyCreationTime > 0) {
//                try {
//                    final InputStream checkpointsInputStream = WalletManager.class.getClassLoader().getResourceAsStream(getNetParams().getId() + ".checkpoints.txt");
//                    CheckpointManager.checkpoint(getNetParams(), checkpointsInputStream, blockStore, earliestKeyCreationTime);
//                } catch (final IOException x) {
//                    LOG.error("problem reading checkpoints, continuing without", x);
//                }
//            }

            List<Wallet> wallets = new ArrayList<>();
            wallets.add(tradeWallet);
            wallets.addAll(readEscrowWallets);

//            BlockChain blockChain = new BlockChain(netParams, escrowWallets, blockStore);
            PeerGroup peerGroup = new PeerGroup(netParams, blockChain);
            peerGroup.setUserAgent("org.bytabit.mobile", AppConfig.getVersion());

            for (Wallet wallet : wallets) {
                peerGroup.addWallet(wallet);
            }

            if (netParams.equals(RegTestParams.get())) {
                peerGroup.setMaxConnections(1);
                peerGroup.addAddress(new PeerAddress(netParams, InetAddress.getLocalHost(), netParams.getPort()));

            } else {
                peerGroup.addPeerDiscovery(new DnsDiscovery(netParams));
            }

            final long earliestKeyCreationTime = tradeWallet.getEarliestKeyCreationTime();
            peerGroup.setFastCatchupTimeSecs(earliestKeyCreationTime);
            return peerGroup;

//        } catch (BlockStoreException bse) {
//            LOG.error("Can't open block store.");
//            throw new RuntimeException(bse);
        } catch (UnknownHostException uhe) {
            //LOG.error("Can't add localhost to peer group.");
            throw new RuntimeException(uhe);
        }
    }

    private void resetBlockchain() {
        // TODO??

//        blockDownloadSubscription.dispose();
//        File blockStoreFile = new File(AppConfig.getPrivateStorage(), "bytabit.spvchain");
//        Boolean blockStoreFileExists = blockStoreFile.exists();
//        if (blockStoreFile.delete()) {
//            peerGroup = createPeerGroup(blockStoreFile, blockStoreFileExists);
//            tradeWallet.reset();
//            for (Wallet escrowWallet : escrowWalletEntries.values()) {
//                escrowWallet.reset();
//            }
//            blockDownloadSubscription = blockDownloadResults.observeOn(Schedulers.io())
//                    .subscribe(e -> {
//                        javafx.application.Platform.runLater(() -> {
//                            if (e instanceof BlockDownloadDone) {
//                                BlockDownloadDone dde = BlockDownloadDone.class.cast(e);
//                                downloadProgress.setValue(1.0);
//                            } else if (e instanceof BlockDownloadProgress) {
//                                BlockDownloadProgress dpe = BlockDownloadProgress.class.cast(e);
//                                downloadProgress.setValue(dpe.getPct());
//                            }
//                            tradeWalletBalance.setValue(getWalletBalance().toFriendlyString());
//                        });
//                    });
//        }
    }

    private Wallet createOrLoadTradeWallet()
            throws FileNotFoundException, UnreadableWalletException {
        final File walletFile = new File(AppConfig.getPrivateStorage(), TRADE_WALLET_FILE_NAME);
        final File walletBackupFile = new File(AppConfig.getPrivateStorage(), TRADE_WALLET_FILE_NAME + BACKUP_EXT);
        return createOrLoadWallet(walletFile, walletBackupFile);
    }

    private Wallet createOrLoadEscrowWallet(String escrowAddress)
            throws FileNotFoundException, UnreadableWalletException {
        File escrowWalletFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME);
        File escrowWalletBackupFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME + BACKUP_EXT);
        return createOrLoadWallet(escrowWalletFile, escrowWalletBackupFile);
    }

    private Wallet createOrLoadWallet(File walletFile, File walletBackupFile)
            throws FileNotFoundException, UnreadableWalletException {
        Context.propagate(btcContext);
        Wallet wallet;
        if (walletFile.exists()) {
            try {
                wallet = loadWallet(walletFile);
//                if (!blockStoreFileExists) {
//                    wallet.reset();
//                }
            } catch (FileNotFoundException | UnreadableWalletException e) {
//                try {
                wallet = loadWallet(walletBackupFile);
                wallet.reset();
                saveWallet(wallet, walletFile);
                // have to remove blockstore file so wallet will be reloaded
//                    if (blockStoreFileExists) {
//                        LOG.debug("Removed blockstore file: {}", blockStoreFile.getName());
//                        blockStoreFile.delete();
//                    }
//                } catch (FileNotFoundException | UnreadableWalletException e1) {
//                    LOG.error("Unable to load backup wallet: {}", walletBackupFile.getName());
//                    throw new RuntimeException(e1);
//                }
            }
        } else {
            // create new wallet
            wallet = new Wallet(netParams);
            // save new wallet
            saveWallet(wallet, walletFile);
            // backup backup new wallet
            backupWallet(wallet, walletBackupFile);
        }

        wallet.autosaveToFile(walletFile, 10, TimeUnit.SECONDS, null);
        return wallet;
    }

    private Wallet loadWallet(File walletFile) throws FileNotFoundException, UnreadableWalletException {
        Context.propagate(btcContext);
        Wallet wallet;
        FileInputStream walletInputStream = null;
        try {
            walletInputStream = new FileInputStream(walletFile);
            final WalletProtobufSerializer serializer = new WalletProtobufSerializer();
            wallet = serializer.readWallet(walletInputStream);
            if (!wallet.getParams().equals(netParams)) {
                //LOG.error("Invalid network params in wallet file: {}", walletFile.toString());
                throw new UnreadableWalletException("Wrong network parameters, found: " + wallet.getParams().getId() + " should be: " + netParams.getId());
            }
            //LOG.debug("Loaded wallet file: {}", walletFile.getName());
        } catch (FileNotFoundException e) {
            //LOG.error("Wallet file does not exist: {}", walletFile.toString());
            throw e;

        } catch (UnreadableWalletException e) {
            //LOG.error("Wallet file is unreadable: {}", walletFile.toString());
            throw e;
        } finally {
            if (walletInputStream != null) {
                try {
                    walletInputStream.close();
                } catch (final IOException x) {
                    //LOG.error("Unable to close wallet input stream: {}", walletFile.toString());
                }
            }
        }

        return wallet;
    }

    private void saveWallet(Wallet wallet, File walletFile) {

        final Protos.Wallet walletProto = new WalletProtobufSerializer().walletToProto(wallet);

        OutputStream walletOutputStream = null;
        try {
            walletOutputStream = new FileOutputStream(walletFile);
            walletProto.writeTo(walletOutputStream);
            walletOutputStream.flush();
            //LOG.info("Wallet saved to: {}", walletFile.getName());
        } catch (final IOException e) {
            //LOG.error("Can't save wallet", e);
        } finally {
            if (walletOutputStream != null) {
                try {
                    walletOutputStream.close();
                } catch (final IOException x) {
                    //LOG.error("Unable to close wallet output stream: {}", walletFile.getName());
                }
            }
        }
    }

    private void backupWallet(Wallet wallet, File walletBackupFile) {

        final Protos.Wallet.Builder walletBuilder = new WalletProtobufSerializer().walletToProto(wallet).toBuilder();

        // strip redundant
        walletBuilder.clearTransaction();
        walletBuilder.clearLastSeenBlockHash();
        walletBuilder.setLastSeenBlockHeight(-1);
        walletBuilder.clearLastSeenBlockTimeSecs();
        final Protos.Wallet walletProto = walletBuilder.build();

        OutputStream walletOutputStream = null;
        try {
            walletOutputStream = new FileOutputStream(walletBackupFile);
            walletProto.writeTo(walletOutputStream);
            walletOutputStream.flush();
            //LOG.info("Wallet backed up to: {}", walletBackupFile.getName());
        } catch (final IOException e) {
            //LOG.error("Can't save wallet backup", e);
        } finally {
            if (walletOutputStream != null) {
                try {
                    walletOutputStream.close();
                } catch (final IOException x) {
                    //LOG.error("Unable to close wallet backup output stream: {}", walletBackupFile.getName());
                }
            }
        }
        //LOG.debug("Backed up wallet to file: {}", walletBackupFile.getName());
    }

//    public DoubleProperty downloadProgressProperty() {
//        return downloadProgress;
//    }
//
//    public ObservableList<TransactionWithAmt> getTradeWalletTransactions() {
//        return tradeWalletTransactions;
//    }
//
//    public StringProperty getTradeWalletBalance() {
//        return tradeWalletBalance;
//    }

    private NetworkParameters getNetParams() {
        return netParams;
    }

    private Address getDepositAddress(Wallet wallet) {
        Context.propagate(btcContext);
        return wallet.currentReceiveAddress();
    }

//    private Single<Address> getFreshReceiveAddress() {
//        return tradeWallet.map(tw -> tw.freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).toAddress(netParams));
//    }

    private String getFreshBase58AuthPubKey(Wallet tradeWallet) {
        Context.propagate(btcContext);
        return Base58.encode(tradeWallet.freshKey(KeyChain.KeyPurpose.AUTHENTICATION).getPubKey());
    }

//    public Single<String> getFreshBase58AuthPubKey() {
//        return Single.fromCallable(() -> Base58.encode(tradeWallet.freshKey(KeyChain.KeyPurpose.AUTHENTICATION).getPubKey()))
//                .subscribeOn(Schedulers.io());
//    }

    private String getFreshBase58ReceivePubKey(Wallet wallet) {
        return Base58.encode(wallet.freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).getPubKey());
    }

//    public Single<Coin> getWalletBalance() {
//        return tradeWallet.map(Wallet::getBalance);
//    }

    private BigDecimal defaultTxFee() {
        return new BigDecimal(defaultTxFeeCoin().toPlainString());
    }

    private Coin defaultTxFeeCoin() {
        return REFERENCE_DEFAULT_MIN_TX_FEE;
    }

    private Single<Transaction> fundEscrow(String escrowAddress, BigDecimal amount) throws InsufficientMoneyException {
        // TODO determine correct amount for extra tx fee for payout, current using DEFAULT_TX_FEE
        return tradeWallet.map(tw -> {
            SendRequest sendRequest = SendRequest.to(Address.fromBase58(netParams, escrowAddress),
                    Coin.parseCoin(amount.toString()).add(defaultTxFeeCoin()));
            return tw.sendCoins(sendRequest).tx;
        });
    }

    private Address escrowAddress(ECKey arbitratorProfilePubKey,
                                  ECKey sellerEscrowPubKey,
                                  ECKey buyerEscrowPubKey) {
        return ScriptBuilder.createP2SHOutputScript(redeemScript(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey)).getToAddress(netParams);
    }

    private String escrowAddress(String arbitratorProfilePubKey,
                                 String sellerEscrowPubKey,
                                 String buyerEscrowPubKey) {
        ECKey apk = ECKey.fromPublicOnly(Base58.decode(arbitratorProfilePubKey));
        ECKey spk = ECKey.fromPublicOnly(Base58.decode(sellerEscrowPubKey));
        ECKey bpk = ECKey.fromPublicOnly(Base58.decode(buyerEscrowPubKey));
        return escrowAddress(apk, spk, bpk).toBase58();
    }

    private Single<ECKey> getECKeyFromAddress(Address address) {
        return tradeWallet.map(tw -> tw.findKeyFromPubHash(address.getHash160()));
    }

    private Single<String> getWatchedOutputAddress(Transaction tx) {
        return tradeWallet.map(tw -> {
            List<String> watchedOutputAddresses = new ArrayList<>();
            for (TransactionOutput output : tx.getOutputs()) {
                Script script = new Script(output.getScriptBytes());
                Address address = output.getAddressFromP2PKHScript(netParams);
                if (address != null && tw.isWatchedScript(script)) {
                    watchedOutputAddresses.add(address.toBase58());
                } else {
                    address = output.getAddressFromP2SH(netParams);
                    if (address != null && tw.isWatchedScript(script)) {
                        watchedOutputAddresses.add(address.toBase58());
                    }
                }
            }

            //if (watchedOutputAddresses.size() > 1) {
            //LOG.error("Found more than one watched output address.");
            //}
            return watchedOutputAddresses.size() > 0 ? watchedOutputAddresses.get(0) : null;
        });
    }

    private String getWatchedOutputAddress(Transaction tx, Wallet wallet) {

        List<String> watchedOutputAddresses = new ArrayList<>();
        for (TransactionOutput output : tx.getOutputs()) {
            Script script = new Script(output.getScriptBytes());
            Address address = output.getAddressFromP2PKHScript(netParams);
            if (address != null && wallet.isWatchedScript(script)) {
                watchedOutputAddresses.add(address.toBase58());
            } else {
                address = output.getAddressFromP2SH(netParams);
                if (address != null && wallet.isWatchedScript(script)) {
                    watchedOutputAddresses.add(address.toBase58());
                }
            }
        }

//        if (watchedOutputAddresses.size() > 1) {
//            LOG.error("Found more than one watched output address.");
//        }
        return watchedOutputAddresses.size() > 0 ? watchedOutputAddresses.get(0) : null;
    }

//    public TransactionWithAmt getTransactionWithAmt(String escrowAddress, String txHash, String toAddress) {
//        TransactionWithAmt transactionWithAmt = null;
//        Sha256Hash hash = Sha256Hash.wrap(txHash);
//        Address addr = Address.fromBase58(netParams, toAddress);
//        Transaction tx = escrowWallets.get(escrowAddress).getTransaction(hash);
//        if (tx == null) {
//            tx = tradeWallet.getTransaction(hash);
//        }
//        if (tx != null) {
//            Coin amt = Coin.ZERO;
//            for (TransactionOutput output : tx.getOutputs()) {
//                if (!addr.isP2SHAddress() && addr.equals(output.getAddressFromP2PKHScript(netParams))
//                        || addr.isP2SHAddress() && addr.equals(output.getAddressFromP2SH(netParams))) {
//                    amt = amt.add(output.getValue());
//                }
//            }
//            transactionWithAmt = new TransactionWithAmtBuilder().tx(tx).transactionAmt(amt).outputAddress(toAddress).inputTxHash(null).build();
//        } else {
//            LOG.error("Can't find Tx with hash: {} to address {}:", txHash, toAddress);
//        }
//
//            return transactionWithAmt;
//        });
//    }

//    public TransactionWithAmt getEscrowTransactionWithAmt(String escrowAddress, String txHash) {
//        Transaction tx = getEscrowTransaction(escrowAddress, txHash);
//        if (tx != null) {
//            TransactionUpdatedEvent txe = TransactionUpdatedEvent.builder().tx(tx).wallet(escrowWalletEntries.get(escrowAddress)).build();
//            return new TransactionWithAmtBuilder().tx(tx).transactionAmt(txe.getAmt()).outputAddress(getWatchedOutputAddress(tx)).inputTxHash(tx.getInput(0).getOutpoint().getHash().toString()).build();
//        } else {
//            return null;
//        }
//}

//    public TransactionWithAmt getTradeTransactionWithAmt(String txHash) {
//        Transaction tx = getTradeTransaction(txHash);
//        if (tx != null) {
//            TransactionUpdatedEvent txe = new TransactionUpdatedEventBuilder().tx(tx).wallet(tradeWallet).build();
//            return new TransactionWithAmtBuilder().tx(tx).transactionAmt(txe.getAmt()).outputAddress(getWatchedOutputAddress(tx)).inputTxHash(tx.getInput(0).getOutpoint().getHash().toString()).build();
//        } else return null;
//    }

//    public Transaction getEscrowTransaction(String escrowAddress, String txHash) {
//        Sha256Hash hash = Sha256Hash.wrap(txHash);
//        Transaction tx = escrowWalletEntries.get(escrowAddress).getTransaction(hash);
//        if (tx == null) {
//            //throw new RuntimeException("Can't find escrow Tx with hash: " + txHash);
//            LOG.error("Can't find escrow Tx with hash to: {}", txHash);
//        }
//        return tx;
//    }

//    public Transaction getTradeTransaction(String txHash) {
//        Sha256Hash hash = Sha256Hash.wrap(txHash);
//        Transaction tx = tradeWallet.getTransaction(hash);
//        if (tx == null) {
//            //throw new RuntimeException("Can't find escrow Tx with hash: " + txHash);
//            LOG.error("Can't find trade Tx with hash: {}", txHash);
//        }
//        return tx;
//    }

    private Single<String> getPayoutSignature(Trade trade, Transaction fundingTx) {
        Address buyerPayoutAddress = Address.fromBase58(netParams, trade.getBuyerPayoutAddress());
        return getPayoutSignature(trade, fundingTx, buyerPayoutAddress);
    }

    private Single<String> getRefundSignature(Trade trade, Transaction fundingTx, Address sellerRefundAddress) {
        return getPayoutSignature(trade, fundingTx, sellerRefundAddress);
    }

    private Single<String> getPayoutSignature(Trade trade, Transaction fundingTx, Address payoutAddress) {
        Coin payoutAmount = Coin.parseCoin(trade.getBtcAmount().toPlainString());
        ECKey arbitratorProfilePubKey = ECKey.fromPublicOnly(Base58.decode(trade.getArbitratorProfilePubKey()));
        ECKey sellerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getSellerEscrowPubKey()));
        ECKey buyerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getBuyerEscrowPubKey()));

        Single<TransactionSignature> signature = getPayoutSignature(payoutAmount, fundingTx,
                arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey,
                payoutAddress);

        return signature.map(sig -> Base58.encode(sig.encodeToBitcoin()));
    }

    private Single<TransactionSignature> getPayoutSignature(Coin payoutAmount,
                                                            Transaction fundingTx,
                                                            ECKey arbitratorProfilePubKey,
                                                            ECKey sellerEscrowPubKey,
                                                            ECKey buyerEscrowPubKey,
                                                            Address payoutAddress) {

        return tradeWallet.map(tw -> {
            Transaction payoutTx = new Transaction(netParams);
            payoutTx.setPurpose(Transaction.Purpose.ASSURANCE_CONTRACT_CLAIM);

            Address escrowAddress = escrowAddress(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey);
            Script redeemScript = redeemScript(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey);

            // add input to payout tx from single matching funding tx output
            for (TransactionOutput txo : fundingTx.getOutputs()) {
                Address outputAddress = txo.getAddressFromP2SH(netParams);
                Coin outputAmount = payoutAmount.plus(defaultTxFeeCoin());

                // verify output from fundingTx exists and equals required payout amounts
                if (outputAddress != null && outputAddress.equals(escrowAddress)
                        && txo.getValue().equals(outputAmount)) {

                    // post payout input and funding output with empty unlock scripts
                    TransactionInput input = payoutTx.addInput(txo);
                    Script emptyUnlockScript = ScriptBuilder.createP2SHMultiSigInputScript(null, redeemScript);
                    input.setScriptSig(emptyUnlockScript);
                    break;
                }
            }

            // add output to payout tx
            payoutTx.addOutput(payoutAmount, payoutAddress);

            // find signing key
            ECKey escrowKey = tw.findKeyFromPubKey(buyerEscrowPubKey.getPubKey());
            if (escrowKey == null) {
                escrowKey = tw.findKeyFromPubKey(sellerEscrowPubKey.getPubKey());
            }
            if (escrowKey == null) {
                escrowKey = tw.findKeyFromPubKey(arbitratorProfilePubKey.getPubKey());
            }
            if (escrowKey != null) {
                // sign tx input
                Sha256Hash unlockSigHash = payoutTx.hashForSignature(0, redeemScript, Transaction.SigHash.ALL, false);
                return new TransactionSignature(escrowKey.sign(unlockSigHash), Transaction.SigHash.ALL, false);
            } else {
                throw new RuntimeException("Can not create payout signature, no signing key found.");
            }
        });
    }

    private static Script redeemScript(ECKey arbitratorProfilePubKey,
                                       ECKey sellerEscrowPubKey,
                                       ECKey buyerEscrowPubKey) {
        return ScriptBuilder.createMultiSigOutputScript(2, ImmutableList.of(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey));
    }

//    public Single<String> payoutEscrowToBuyer(Trade trade) throws InsufficientMoneyException {
//
//        Address buyerPayoutAddress = Address.fromBase58(netParams, trade.getBuyerPayoutAddress());
//
//        String fundingTxHash = trade.getFundingTxHash();
//        Transaction fundingTx = getEscrowTransaction(trade.getEscrowAddress(), fundingTxHash);
//
//        String signature = getPayoutSignature(trade, fundingTx, buyerPayoutAddress);
//
//        TransactionSignature sellerSignature = TransactionSignature
//                .decodeFromBitcoin(Base58.decode(signature), true, true);
//
//        TransactionSignature buyerSignature = TransactionSignature
//                .decodeFromBitcoin(Base58.decode(trade.getPayoutTxSignature()), true, true);
//
//        List<TransactionSignature> signatures = ImmutableList.of(sellerSignature, buyerSignature);
//
//        return payoutEscrow(trade, buyerPayoutAddress, signatures);
//    }
//
//    public String refundEscrowToSeller(Trade trade) throws InsufficientMoneyException {
//
//        Address sellerRefundAddress = Address.fromBase58(netParams, trade.getRefundAddress());
//
//        String fundingTxHash = trade.getFundingTxHash();
//        Transaction fundingTx = getEscrowTransaction(trade.getEscrowAddress(), fundingTxHash);
//
//        String signature = getPayoutSignature(trade, fundingTx, sellerRefundAddress);
//
//        TransactionSignature arbitratorSignature = TransactionSignature
//                .decodeFromBitcoin(Base58.decode(signature), true, true);
//
//        TransactionSignature sellerSignature = TransactionSignature
//                .decodeFromBitcoin(Base58.decode(trade.getRefundTxSignature()), true, true);
//
//        List<TransactionSignature> signatures = ImmutableList.of(arbitratorSignature, sellerSignature);
//
//        return payoutEscrow(trade, sellerRefundAddress, signatures);
//    }
//
//    public String cancelEscrowToSeller(Trade trade) throws InsufficientMoneyException {
//
//        Address sellerRefundAddress = Address.fromBase58(netParams, trade.getRefundAddress());
//
//        String fundingTxHash = trade.getFundingTxHash();
//        Transaction fundingTx = getEscrowTransaction(trade.getEscrowAddress(), fundingTxHash);
//
//        String signature = getPayoutSignature(trade, fundingTx, sellerRefundAddress);
//
//        TransactionSignature sellerSignature = TransactionSignature
//                .decodeFromBitcoin(Base58.decode(trade.getRefundTxSignature()), true, true);
//
//        TransactionSignature buyerSignature = TransactionSignature
//                .decodeFromBitcoin(Base58.decode(signature), true, true);
//
//        List<TransactionSignature> signatures = ImmutableList.of(sellerSignature, buyerSignature);
//
//        return payoutEscrow(trade, sellerRefundAddress, signatures);
//    }
//
//    private String payoutEscrow(Trade trade, Address payoutAddress,
//                                List<TransactionSignature> signatures) {
//
//        String fundingTxHash = trade.getFundingTxHash();
//        Transaction fundingTx = getEscrowTransaction(trade.getEscrowAddress(), fundingTxHash);
//
//        if (fundingTx != null) {
//
//            Transaction payoutTx = new Transaction(netParams);
//            payoutTx.setPurpose(Transaction.Purpose.ASSURANCE_CONTRACT_CLAIM);
//            Coin payoutAmount = Coin.parseCoin(trade.getBtcAmount().toPlainString());
//
//            ECKey arbitratorProfilePubKey = ECKey.fromPublicOnly(Base58.decode(trade.getArbitratorProfilePubKey()));
//            ECKey sellerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getSellerEscrowPubKey()));
//            ECKey buyerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getBuyerEscrowPubKey()));
//
//            Script redeemScript = redeemScript(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey);
//            Address escrowAddress = escrowAddress(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey);
//
//            // add input to payout tx from single matching funding tx output
//            for (TransactionOutput txo : fundingTx.getOutputs()) {
//                Address outputAddress = txo.getAddressFromP2SH(netParams);
//                Coin outputAmount = payoutAmount.plus(defaultTxFeeCoin());
//
//                // verify output from fundingTx exists and equals required payout amounts
//                if (outputAddress != null && outputAddress.equals(escrowAddress)
//                        && txo.getValue().equals(outputAmount)) {
//
//                    // post payout input and funding output with signed unlock script
//                    TransactionInput input = payoutTx.addInput(txo);
//                    Script signedUnlockScript = ScriptBuilder.createP2SHMultiSigInputScript(signatures, redeemScript);
//                    input.setScriptSig(signedUnlockScript);
//                    break;
//                }
//            }
//
//            // add output to payout tx
//            payoutTx.addOutput(payoutAmount, payoutAddress);
//
//            LOG.debug("Validate inputs for payoutTx: {}", payoutTx.toString());
//            for (TransactionInput input : payoutTx.getInputs()) {
//                LOG.debug("Validating input for payoutTx: {}", input.toString());
//                try {
//                    input.verify(input.getConnectedOutput());
//                    LOG.debug("Input valid for payoutTx: {}", input.toString());
//                } catch (VerificationException ve) {
//                    LOG.error("Input not valid for payoutTx, {}", ve.getMessage());
//                } catch (NullPointerException npe) {
//                    LOG.error("Null connectedOutput for payoutTx");
//                }
//            }
//
//            escrowWalletEntries.get(trade.getEscrowAddress()).commitTx(payoutTx);
//            peerGroup.broadcastTransaction(payoutTx);
//            return payoutTx.getHash().toString();
//        } else {
//            // TODO reset blockchain and reload, then retry...
//            LOG.error("No funding tx found for payout tx.");
//            throw new RuntimeException("No funding tx found for payout tx.");
//        }
//    }
//
//    public String sendCoins(SendRequest sendRequest) throws InsufficientMoneyException {
//        Transaction tx = tradeWallet.sendCoins(sendRequest).tx;
//        return tx.getHashAsString();
//    }

    private Wallet createEscrowWallet(String escrowAddress)
            throws FileNotFoundException, UnreadableWalletException {

        Context.propagate(btcContext);
        Address address = Address.fromBase58(getNetParams(), escrowAddress);
//        if (escrowTxUpdatedEventEntries.containsKey(escrowAddress)) {
//            if (!escrowWalletEntries.containsKey(escrowAddress)) {

        File tradeDir = new File(TRADES_PATH + escrowAddress);
        if (!tradeDir.exists()) {
            tradeDir.mkdirs();
//            LOG.debug("Created new trade dir: {}", tradeDir);
        }
        final File escrowWalletFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME);
        final File escrowWalletBackupFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME + BACKUP_EXT);
//                final File blockStoreFile = new File(AppConfig.getPrivateStorage(), "bytabit.spvchain");

        Wallet escrowWallet = createOrLoadWallet(escrowWalletFile, escrowWalletBackupFile);
        escrowWallet.addWatchedAddress(address, (DateTime.now().getMillis() / 1000));
        return escrowWallet;

        //                escrowWalletEntries.put(escrowAddress, escrowWallet);

//                Observable<TransactionUpdatedEvent> escrowWalletTxObservable = Observable.<TransactionUpdatedEvent>create(source -> {
//                    TransactionConfidenceEventListener listener = new TransactionConfidenceEventListener() {
//
//                        @Override
//                        public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
//                            Context.propagate(btcContext);
//                            source.onNext(new TransactionUpdatedEventBuilder().tx(tx).wallet(wallet).build());
//                        }
//                    };
//                    escrowWallet.addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);
//
//                    source.setCancellable(() -> escrowWallet.removeTransactionConfidenceEventListener(listener));
//                }).share();

//                escrowTxUpdatedEventEntries.put(escrowAddress, escrowWalletTxObservable);
//                escrowWalletTxObservable.observeOn(Schedulers.io())
//                        .subscribe(e -> {
//                            LOG.debug("escrow {} transaction updated event : {}", escrowAddress, e);
//                        });
//
//                blockChain.addWallet(escrowWallet);
//                peerGroup.addWallet(escrowWallet);
//            }
//        if (escrowWalletEntries.get(escrowAddress).addWatchedAddress(address, (DateTime.now().getMillis() / 1000))) {
//            LOG.debug("Added watched address: {}", address.toBase58());
//        } else {
//            LOG.warn("Failed to add watch address: {}", address.toBase58());
//        }
//        }
//        return escrowTxUpdatedEventEntries.get(escrowAddress);
    }

    //    public void removeWatchedEscrowAddress(String escrowAddress) {
//        Address address = Address.fromBase58(getNetParams(), escrowAddress);
//        Context.propagate(btcContext);
//        if (escrowWalletEntries.containsKey(escrowAddress)) {
//            escrowTxUpdatedEventEntries.get(escrowAddress).unsubscribeOn(Schedulers.io());
//            blockChain.removeWallet(escrowWalletEntries.get(escrowAddress));
//            peerGroup.removeWallet(escrowWalletEntries.get(escrowAddress));
//            escrowWalletEntries.get(escrowAddress).shutdownAutosaveAndWait();
//
//            // rename wallet file
//            final File escrowWalletFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME);
//            final File escrowWalletSavFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME + SAVE_EXT);
//
//            if (escrowWalletFile.renameTo(escrowWalletSavFile)) {
//                if (escrowWalletFile.exists() && escrowWalletFile.delete()) {
//                    LOG.debug("Deleted {}", escrowWalletFile);
//                } else {
//                    LOG.error("Failed to delete {}", escrowWalletFile);
//                }
//            } else {
//                LOG.error("Failed to rename {} to {}", escrowWalletFile, escrowWalletSavFile);
//            }
//
//            LOG.debug("Removed watched escrow address: {}", address.toBase58());
//        } else {
//            LOG.warn("Failed to remove watched escrow address: {}", address.toBase58());
//        }
//    }
//
    private String getSeedWords(Wallet wallet) {
        return Joiner.on(" ").join(wallet.getKeyChainSeed().getMnemonicCode());
    }

    private String getXprvKey(Wallet wallet) {
        return wallet.getWatchingKey().serializePrivB58(netParams);
    }

    private String getXpubKey(Wallet wallet) {
        return wallet.getWatchingKey().serializePubB58(netParams);
    }

    public PublishSubject<WalletAction> getActions() {
        return actions;
    }

    public Observable<WalletResult> getWalletResults() {
        return walletResults;
    }

    public ConnectableObservable<BlockDownloadResult> getBlockDownloadResults() {
        return blockDownloadResults;
    }

    public ConnectableObservable<TransactionResult> getTradeWalletTransactionResults() {
        return tradeWalletTransactionResults;
    }

    //    public Observable<BlockDownloadResult> getBlockDownloadResults() {
//        return blockDownloadResults;
//    }

//    public Observable<Double> getBlockDownloadProgress() {
//        return blockDownloadResults.filter(bde -> bde instanceof BlockDownloadProgress)
//                .cast(BlockDownloadProgress.class).map(BlockDownloadProgress::getPct);
//    }

//    public Observable<String> getTradeWalletBalance() {
//        return tradeWalletBalance;
//    }

//    public Observable<TransactionResult> getTradeWalletTransactionResults() {
//        return tradeWalletTransactionResults;
//    }

    // Wallet Action classes

    public interface WalletAction extends Event {
    }

    public class GetTradeWalletInfo implements WalletAction {
    }

    public class GetTradeWalletDepositAddress implements WalletAction {
    }

    public class GetProfilePubKey implements WalletAction {
    }

    public class GetEscrowPubKey implements WalletAction {
    }

    // Wallet Result Classes

    public interface WalletResult extends Result {
    }

    public class TradeWalletInfo implements WalletResult {
        private final String seedWords;
        private final String xpubKey;
        private final String xprvKey;

        public TradeWalletInfo(String seedWords, String xpubKey, String xprvKey) {
            this.seedWords = seedWords;
            this.xpubKey = xpubKey;
            this.xprvKey = xprvKey;
        }

        public String getSeedWords() {
            return seedWords;
        }

        public String getXpubKey() {
            return xpubKey;
        }

        public String getXprvKey() {
            return xprvKey;
        }
    }

    public class TradeWalletDepositAddress implements WalletResult {
        private final Address address;

        public TradeWalletDepositAddress(Address address) {
            this.address = address;
        }

        public Address getAddress() {
            return address;
        }
    }

    public class ProfilePubKey implements WalletResult {
        private final String pubKey;

        public ProfilePubKey(String pubKey) {
            this.pubKey = pubKey;
        }

        public String getPubKey() {
            return pubKey;
        }
    }

    public class EscrowPubKey implements WalletResult {
        private final String pubKey;

        public EscrowPubKey(String pubKey) {
            this.pubKey = pubKey;
        }

        public String getPubKey() {
            return pubKey;
        }
    }

    // Block Download Results

    public interface BlockDownloadResult extends Result {
    }

    public class BlockDownloadUpdate implements BlockDownloadResult {
        private final Double percent;

        public BlockDownloadUpdate(Double percent) {
            this.percent = percent;
        }

        public Double getPercent() {
            return percent;
        }
    }

    public class BlockDownloadDone implements BlockDownloadResult {
    }

    public class BlockDownloadError implements BlockDownloadResult, ErrorResult {
        private final Throwable error;

        public BlockDownloadError(Throwable error) {
            this.error = error;
        }

        @Override
        public Throwable getError() {
            return error;
        }
    }

    // Transaction Results

    public interface TransactionResult extends Result {
    }

    public class TradeWalletUpdate implements TransactionResult {
        private final TransactionWithAmt transactionWithAmt;

        public TradeWalletUpdate(TransactionWithAmt transactionWithAmt) {
            this.transactionWithAmt = transactionWithAmt;
        }

        public TransactionWithAmt getTransactionWithAmt() {
            return transactionWithAmt;
        }
    }

    public class TradeWalletError implements TransactionResult, ErrorResult {
        private final Throwable error;

        public TradeWalletError(Throwable error) {
            this.error = error;
        }

        @Override
        public Throwable getError() {
            return error;
        }
    }

    public class EscrowWalletUpdate implements TransactionResult {
        private final String escrowAddress;
        private final TransactionWithAmt transactionWithAmt;

        public EscrowWalletUpdate(String escrowAddress, TransactionWithAmt transactionWithAmt) {
            this.escrowAddress = escrowAddress;
            this.transactionWithAmt = transactionWithAmt;
        }

        public String getEscrowAddress() {
            return escrowAddress;
        }

        public TransactionWithAmt getTransactionWithAmt() {
            return transactionWithAmt;
        }
    }

    public class EscrowWalletError implements TransactionResult, ErrorResult {
        private final String escrowAddress;
        private final Throwable error;

        public EscrowWalletError(String escrowAddress, Throwable error) {
            this.escrowAddress = escrowAddress;
            this.error = error;
        }

        public String getEscrowAddress() {
            return escrowAddress;
        }

        @Override
        public Throwable getError() {
            return error;
        }
    }
}
