package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.nav.evt.QuitEvent;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.model.ManagedWallets;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bytabit.mobile.trade.manager.TradeManager.TRADES_PATH;
import static com.bytabit.mobile.wallet.manager.WalletManager.Command.*;
import static org.bitcoinj.core.Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;

public class WalletManager {

    public enum Command {
        START,
        STOP,
        RESET
    }

    final static String TRADE_WALLET_FILE_NAME = "trade.wallet";
    final static String ESCROW_WALLET_FILE_NAME = "escrow.wallet";
    final static String BACKUP_EXT = ".bkp";
    //private final static String SAVE_EXT = ".sav";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final NetworkParameters netParams;
    private final Context btcContext;

    private final Subject<Command> commandSubject;

    private ConnectableObservable<ManagedWallets> managedWallets;

    private ConnectableObservable<Double> downloadProgress;

    private ConnectableObservable<TransactionWithAmt> updatedTradeWalletTx;
    private ConnectableObservable<TransactionWithAmt> updatedEscrowWalletTx;

    public WalletManager() {
        netParams = NetworkParameters.fromID("org.bitcoin." + AppConfig.getBtcNetwork());
        btcContext = Context.getOrCreate(netParams);
        commandSubject = PublishSubject.create();
    }

    @PostConstruct
    public void initialize() {

        managedWallets = commandSubject
                .startWith(START)
                .doOnNext(c -> log.debug("command: {}", c))
                .switchMap(command -> {
                    if (START.equals(command)) {
                        return createManagedWallets(false);
                    } else if (RESET.equals(command)) {
                        return createManagedWallets(true);
                    } else {
                        return Observable.empty();
                    }
                })
                .doOnTerminate(() -> log.debug("managedWallets: terminate"))
                .doOnDispose(() -> log.debug("managedWallets: dispose"))
                .doOnSubscribe(d -> log.debug("managedWallets: subscribe"))
                .doOnNext(mw -> log.debug("managedWallets: {}", mw))
                .replay(1);

        updatedTradeWalletTx = managedWallets.autoConnect()
                .map(ManagedWallets::getTradeWallet)
                .switchMap(tw -> Observable.<TransactionWithAmt>create(source -> {

                    Observable.fromIterable(tw.getTransactions(false))
                            .subscribe(tx -> source.onNext(createTransactionWithAmt(tw, tx)));

                    TransactionConfidenceEventListener listener = (wallet, tx) -> {
                        source.onNext(createTransactionWithAmt(wallet, tx));
                    };

                    tw.addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);

                    source.setCancellable(() -> {
                        log.debug("updatedTradeWalletTx: removeTransactionConfidenceEventListener");
                        tw.removeTransactionConfidenceEventListener(listener);
                    });
                }).groupBy(TransactionWithAmt::getHash).flatMap(txg ->
                        txg.throttleLast(1, TimeUnit.SECONDS)))
                .doOnSubscribe(d -> log.debug("updatedTradeWalletTx: subscribe"))
                .doOnNext(tx -> log.debug("updatedTradeWalletTx: {}", tx.getHash()))
                .replay(20, TimeUnit.MINUTES);

//        updatedEscrowWalletTx = managedWallets.autoConnect()
//                .flatMapIterable(ManagedWallets::getEscrowWallets)
//                .switchMap(ew -> Observable.<TransactionWithAmt>create(source -> {
//                    TransactionConfidenceEventListener listener = (wallet, tx) -> {
//                        Context.propagate(btcContext);
//                        String watchedEscrowAddress = wallet.getWatchedScripts().get(0).getToAddress(netParams).toBase58();
//                        TransactionWithAmt txWithAmt = TransactionWithAmt.builder()
//                                .tx(tx)
//                                .transactionAmt(tx.getValue(wallet))
//                                .outputAddress(getWatchedOutputAddress(tx, wallet))
//                                .inputTxHash(tx.getInput(0).getOutpoint().getHash().toString())
//                                .walletBalance(ew.getBalance())
//                                .escrowAddress(watchedEscrowAddress)
//                                .build();
//                        source.onNext(txWithAmt);
//                    };
//                    ew.addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);
//
//                    source.setCancellable(() -> {
//                        ew.removeTransactionConfidenceEventListener(listener);
//                    });
//                }).groupBy(TransactionWithAmt::getHash).flatMap(txg ->
//                        txg.throttleLast(1, TimeUnit.SECONDS)))
//                .doOnSubscribe(d -> log.debug("updatedEscrowWalletTx: subscribe"))
//                .doOnNext(tx -> log.debug("updatedEscrowWalletTx: {}", tx))
//                .replay(20, TimeUnit.MINUTES);

        // post observable download events
        downloadProgress = managedWallets.autoConnect()
                .map(ManagedWallets::getPeerGroup)
                .switchMap(pg -> Observable.<Double>create(source -> {

                    pg.startBlockChainDownload(new DownloadProgressTracker() {
                        @Override
                        public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {
                            super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft);
                        }

                        @Override
                        protected void progress(double pct, int blocksSoFar, Date date) {
                            super.progress(pct, blocksSoFar, date);
                            source.onNext(pct / 100.00);
                        }

                        @Override
                        protected void doneDownload() {
                            super.doneDownload();
                            source.onNext(1.00);
                        }
                    });

                    // on un-subscribe stop peer group
                    source.setCancellable(pg::stop);
                })).subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> log.debug("downloadProgress: subscribe"))
                .doOnNext(progress -> log.debug("downloadProgress: {}%", BigDecimal.valueOf(progress * 100.00).setScale(0, BigDecimal.ROUND_DOWN)))
                .throttleLast(1, TimeUnit.SECONDS)
                .replay(100);

        BytabitMobile.getNavEvents().distinctUntilChanged()
                .filter(ne -> ne instanceof QuitEvent)
                .subscribe(qe -> commandSubject.onNext(STOP));
    }

    private Observable<ManagedWallets> createManagedWallets(boolean deleteBlockstore) {

        // create trade wallet

        Observable<Wallet> tradeWallet = Observable.<Wallet>create(source -> {
            try {
                source.onNext(createOrLoadTradeWallet());
            } catch (IOException | UnreadableWalletException e) {
                source.onError(e);
            }
        }).cache();

        // create escrow wallets

        Single<File> tradesDir = Single.just(new File(TRADES_PATH));

        Observable<String> readEscrowAddresses = tradesDir
                .map(td -> Arrays.asList(td != null && td.list() != null ? td.list() : new String[0]))
                .flattenAsObservable(escrowAddresses -> escrowAddresses);

        Observable<Wallet> escrowWallets = readEscrowAddresses
                .map(this::createOrLoadEscrowWallet).cache();

        // create block store

        Single<File> blockStoreFile = Single.just(new File(AppConfig.getPrivateStorage(), "bytabit.spvchain"));

        Observable<BlockStore> blockStore = blockStoreFile.toObservable().map(bsf -> {
            Context.propagate(btcContext);
            if (deleteBlockstore && bsf.exists()) {
                bsf.delete();
            }
            BlockStore bs = new SPVBlockStore(netParams, bsf);
            bs.getChainHead(); // detect corruptions as early as possible
            return bs;
        }).doOnError(t -> {
            blockStoreFile.subscribe(File::delete);
        }).retry(1);

        // create block chain

        Observable<BlockChain> blockChain = Observable.zip(tradeWallet, escrowWallets.toList().toObservable(), blockStore, (tw, ew, bs) -> {
            Context.propagate(btcContext);
            List<Wallet> wallets = new ArrayList<>();
            wallets.add(tw);
            wallets.addAll(ew);
            return new BlockChain(netParams, wallets, bs);
        });

        // create peer group

        Observable<PeerGroup> peerGroup = Observable.zip(tradeWallet, escrowWallets.toList().toObservable(), blockChain, (tw, ew, bc) -> {
            Context.propagate(btcContext);
            PeerGroup pg = createPeerGroup(bc, tw, ew);
            pg.start();
            return pg;
        });

        return Observable.zip(tradeWallet, escrowWallets.toList().toObservable(), peerGroup, ManagedWallets::new)
                .doOnDispose(() -> {
                    log.debug("createManagedWallets: dispose");
                    tradeWallet.doOnDispose(() -> log.debug("disposed trade wallet")).subscribe(Wallet::shutdownAutosaveAndWait);
                    escrowWallets.doOnDispose(() -> log.debug("disposed escrow wallet")).subscribe(Wallet::shutdownAutosaveAndWait);
                    peerGroup.doOnDispose(() -> log.debug("disposed peer group")).subscribe(PeerGroup::stop);
                });
    }

    public ConnectableObservable<Double> getDownloadProgress() {
        return downloadProgress;
    }

    public Observable<TradeWalletInfo> getTradeWalletInfo() {
        return managedWallets.autoConnect().map(ManagedWallets::getTradeWallet)
                .map(this::getTradeWalletInfo);
    }

    public ConnectableObservable<TransactionWithAmt> getUpdatedTradeWalletTx() {
        return updatedTradeWalletTx;
    }

    public ConnectableObservable<TransactionWithAmt> getUpdatedEscrowWalletTx() {
        return updatedEscrowWalletTx;
    }

    private TransactionWithAmt createTransactionWithAmt(Wallet wallet, Transaction tx) {
        Context.propagate(btcContext);
        return TransactionWithAmt.builder()
                .tx(tx)
                .transactionAmt(tx.getValue(wallet))
                .outputAddress(getWatchedOutputAddress(tx, wallet))
                .inputTxHash(tx.getInput(0).getOutpoint().getHash().toString())
                .walletBalance(wallet.getBalance())
                .build();
    }

    public Maybe<String> getTradeWalletProfilePubKey() {
        return managedWallets.autoConnect().firstElement()
                .map(ManagedWallets::getTradeWallet).map(this::getFreshBase58AuthPubKey);
    }

    public Maybe<String> getTradeWalletEscrowPubKey() {
        return managedWallets.autoConnect().firstElement()
                .map(ManagedWallets::getTradeWallet).map(this::getFreshBase58ReceivePubKey);
    }

    public Maybe<Address> getTradeWalletDepositAddress() {
        return managedWallets.autoConnect().firstElement()
                .map(ManagedWallets::getTradeWallet).map(this::getDepositAddress);
    }

    private PeerGroup createPeerGroup(BlockChain blockChain, Wallet tradeWallet, List<Wallet> readEscrowWallets) throws UnknownHostException {

        List<Wallet> wallets = new ArrayList<>();
        wallets.add(tradeWallet);
        wallets.addAll(readEscrowWallets);

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
    }

    private Wallet createOrLoadTradeWallet()
            throws IOException, UnreadableWalletException {

        final File walletFile = new File(AppConfig.getPrivateStorage(), TRADE_WALLET_FILE_NAME);
        final File walletBackupFile = new File(AppConfig.getPrivateStorage(), TRADE_WALLET_FILE_NAME + BACKUP_EXT);
        return createOrLoadWallet(walletFile, walletBackupFile);
    }

    public Maybe<String> createEscrowWallet(String escrowAddress, boolean resetBlockchain) {

        return Maybe.create(source -> {
            try {
                createOrLoadEscrowWallet(escrowAddress);
                if (resetBlockchain) {
                    commandSubject.onNext(RESET);
                } else {
                    commandSubject.onNext(START);
                }
                source.onSuccess(escrowAddress);
            } catch (FileNotFoundException | UnreadableWalletException ex) {
                source.onError(ex);
            }
        });
    }

    private Wallet createOrLoadEscrowWallet(String escrowAddress)
            throws IOException, UnreadableWalletException {
        Context.propagate(btcContext);
        File tradeDir = new File(TRADES_PATH + escrowAddress);
        if (!tradeDir.exists()) {
            tradeDir.mkdirs();
        }
        File escrowWalletFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME);
        File escrowWalletBackupFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME + BACKUP_EXT);
        return createOrLoadWallet(escrowWalletFile, escrowWalletBackupFile, escrowAddress);
    }

    Wallet createOrLoadWallet(File walletFile, File walletBackupFile)
            throws IOException, UnreadableWalletException {

        return createOrLoadWallet(walletFile, walletBackupFile, null);
    }

    Wallet createOrLoadWallet(File walletFile, File walletBackupFile, String escrowAddress)
            throws IOException, UnreadableWalletException {

        Context.propagate(btcContext);
        Wallet wallet;
        if (walletFile.exists()) {
            try {
                wallet = loadWallet(walletFile);

            } catch (FileNotFoundException | UnreadableWalletException e1) {

                try {
                    wallet = loadWallet(walletBackupFile);
                    wallet.reset();
                    log.debug("restoreWallet (" + e1.getMessage() + "): " + walletBackupFile.getPath());
                    saveWallet(wallet, walletFile);

                    // TODO reset wallets and delete and reload block store file

                } catch (IOException | UnreadableWalletException e2) {
                    log.error("Unable to load backup wallet: {}", walletBackupFile.getPath());
                    throw e2;
                }
            }
        } else {
            log.debug("createWallet: " + walletFile.getPath());
            // create new wallet
            wallet = new Wallet(netParams);
            if (escrowAddress != null) {
                wallet.addWatchedAddress(Address.fromBase58(netParams, escrowAddress), (DateTime.now().getMillis() / 1000));
            }
            // save new wallet
            saveWallet(wallet, walletFile);
            // backup backup new wallet
            backupWallet(wallet, walletBackupFile);
        }

        wallet.autosaveToFile(walletFile, 10, TimeUnit.SECONDS, null);
        return wallet;
    }

    private Wallet loadWallet(File walletFile) throws IOException, UnreadableWalletException {

        Context.propagate(btcContext);
        Wallet wallet;
        FileInputStream walletInputStream = null;

        walletInputStream = new FileInputStream(walletFile);
        final WalletProtobufSerializer serializer = new WalletProtobufSerializer();
        wallet = serializer.readWallet(walletInputStream);

        if (!wallet.getParams().equals(netParams)) {
            log.error("Invalid network params in wallet file: {}", walletFile.toString());
            throw new UnreadableWalletException("Wrong network parameters, found: " + wallet.getParams().getId() + " should be: " + netParams.getId());
        }
        log.debug("loadWallet: {}", walletFile.getPath());

        try {
            walletInputStream.close();
        } catch (final IOException e) {
            log.error("Unable to close wallet input stream: {}", walletFile.toString());
            throw e;
        }

        return wallet;
    }

    private void saveWallet(Wallet wallet, File walletFile) throws IOException {

        final Protos.Wallet walletProto = new WalletProtobufSerializer().walletToProto(wallet);

        OutputStream walletOutputStream = null;
        try {
            walletOutputStream = new FileOutputStream(walletFile);
            walletProto.writeTo(walletOutputStream);
            walletOutputStream.flush();
            log.debug("savedWallet: {}", walletFile.getPath());
        } finally {
            if (walletOutputStream != null) {
                walletOutputStream.close();
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
    }

    private Maybe<Wallet> getTradeWallet() {
        return managedWallets.autoConnect().firstElement()
                .map(ManagedWallets::getTradeWallet);
    }

    private Maybe<Wallet> getEscrowWallet(String escrowAddress) {

        return managedWallets.autoConnect().firstElement()
                .flattenAsObservable(ManagedWallets::getEscrowWallets)
                .filter(ew -> escrowAddress.equals(escrowAddress(ew)))
                .firstElement();
    }

    private Maybe<PeerGroup> getPeerGroup() {

        return managedWallets.autoConnect().firstElement()
                .map(ManagedWallets::getPeerGroup);
    }

    private String escrowAddress(Wallet escrowWallet) {

        if (escrowWallet.getWatchedScripts().size() != 1) {
            throw new RuntimeException("Escrow wallet does not have single watch script");
        }

        Address escrowAddress = escrowWallet.getWatchedScripts().get(0).getToAddress(netParams);

        // make sure there is only watched address and it is a P2SH address
        if (!escrowAddress.isP2SHAddress()) {
            throw new RuntimeException("Escrow wallet does not have P2SH watch address");
        }
        return escrowAddress.toBase58();
    }

    private NetworkParameters getNetParams() {
        return netParams;
    }

    private Address getDepositAddress(Wallet wallet) {
        Context.propagate(btcContext);
        return wallet.currentReceiveAddress();
    }

    private String getFreshBase58AuthPubKey(Wallet tradeWallet) {
        Context.propagate(btcContext);
        return Base58.encode(tradeWallet.freshKey(KeyChain.KeyPurpose.AUTHENTICATION).getPubKey());
    }

    private TradeWalletInfo getTradeWalletInfo(Wallet tradeWallet) {
        Context.propagate(btcContext);
        return new TradeWalletInfo(getSeedWords(tradeWallet), getXpubKey(tradeWallet), getXprvKey(tradeWallet));
    }

    private String getFreshBase58ReceivePubKey(Wallet wallet) {
        Context.propagate(btcContext);
        return Base58.encode(wallet.freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).getPubKey());
    }

    public BigDecimal defaultTxFee() {
        return new BigDecimal(defaultTxFeeCoin().toPlainString());
    }

    private Coin defaultTxFeeCoin() {
        return REFERENCE_DEFAULT_MIN_TX_FEE.multiply(100);
    }

    public Maybe<Transaction> fundEscrow(String escrowAddress, BigDecimal amount) {
        Context.propagate(btcContext);

        // TODO determine correct amount for extra tx fee for payout, current using DEFAULT_TX_FEE
        return getEscrowWallet(escrowAddress)
                .filter(ew -> ew.getBalance(Wallet.BalanceType.ESTIMATED).compareTo(Coin.ZERO) == 0)
                .flatMap(ew -> getTradeWallet())
                .flatMap(tw -> Maybe.create(source -> {
                    try {
                        SendRequest sendRequest = SendRequest.to(Address.fromBase58(netParams, escrowAddress),
                                Coin.parseCoin(amount.toString()).add(defaultTxFeeCoin()));
                        Wallet.SendResult sendResult = tw.sendCoins(sendRequest);
                        source.onSuccess(sendResult.tx);
                    } catch (InsufficientMoneyException ex) {
                        log.error("Insufficient BTC to fund trade escrow.");
                        // TODO let user know not enough BTC in wallet
                        source.onError(ex);
                    }
                }));
    }

    private Address escrowAddress(ECKey arbitratorProfilePubKey,
                                  ECKey sellerEscrowPubKey,
                                  ECKey buyerEscrowPubKey) {
        return ScriptBuilder.createP2SHOutputScript(redeemScript(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey)).getToAddress(netParams);
    }

    public String escrowAddress(String arbitratorProfilePubKey,
                                String sellerEscrowPubKey,
                                String buyerEscrowPubKey) {
        ECKey apk = ECKey.fromPublicOnly(Base58.decode(arbitratorProfilePubKey));
        ECKey spk = ECKey.fromPublicOnly(Base58.decode(sellerEscrowPubKey));
        ECKey bpk = ECKey.fromPublicOnly(Base58.decode(buyerEscrowPubKey));
        return escrowAddress(apk, spk, bpk).toBase58();
    }

    private Maybe<ECKey> getECKeyFromAddress(Address address) {
        return getTradeWallet().map(tw -> tw.findKeyFromPubHash(address.getHash160()));
    }

    private Maybe<String> getWatchedOutputAddress(Transaction tx) {
        return getTradeWallet().map(tw -> {
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

            return watchedOutputAddresses.size() > 0 ? watchedOutputAddresses.get(0) : null;
        });
    }

    private String getWatchedOutputAddress(Transaction tx, Wallet wallet) {

        Address address = null;
        // TODO find a more elegant way to determine if this is a funding or payout escrow transaction
        if (tx.getOutputs().size() == 1) {
            TransactionOutput output = tx.getOutputs().get(0);
            address = output.getAddressFromP2PKHScript(netParams);
            if (address != null) {
                return address.toBase58();
            }
        }
        List<String> watchedOutputAddresses = new ArrayList<>();
        for (TransactionOutput output : tx.getOutputs()) {
            Script script = new Script(output.getScriptBytes());
            address = output.getAddressFromP2PKHScript(netParams);
            if (address != null && wallet.isWatchedScript(script)) {
                watchedOutputAddresses.add(address.toBase58());
            } else {
                address = output.getAddressFromP2SH(netParams);
                if (address != null && wallet.isWatchedScript(script)) {
                    watchedOutputAddresses.add(address.toBase58());
                }
            }
        }

        return watchedOutputAddresses.size() > 0 ? watchedOutputAddresses.get(0) : null;
    }

    public Maybe<TransactionWithAmt> getEscrowTransactionWithAmt(String escrowAddress, String txHash) {

        return getEscrowWallet(escrowAddress).flatMap(w -> {
            Transaction tx = txHash != null ? w.getTransaction(Sha256Hash.wrap(txHash)) : null;
            Maybe<Transaction> maybeTx = tx != null ? Maybe.just(tx) : Maybe.empty();
            return maybeTx.map(t -> createTransactionWithAmt(w, t));
        });
    }

    public Maybe<String> getPayoutSignature(Trade fundedTrade) {
        Address buyerPayoutAddress = Address.fromBase58(netParams, fundedTrade.getBuyerPayoutAddress());
        return getPayoutSignature(fundedTrade, fundedTrade.fundingTransactionWithAmt().getTransaction(), buyerPayoutAddress);
    }

    public Maybe<String> getRefundSignature(Trade trade, Transaction
            fundingTx, Address sellerRefundAddress) {
        return getPayoutSignature(trade, fundingTx, sellerRefundAddress);
    }

    private Maybe<String> getPayoutSignature(Trade trade, Transaction
            fundingTx, Address payoutAddress) {
        Coin payoutAmount = Coin.parseCoin(trade.getBtcAmount().toPlainString());
        ECKey arbitratorProfilePubKey = ECKey.fromPublicOnly(Base58.decode(trade.getArbitratorProfilePubKey()));
        ECKey sellerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getSellerEscrowPubKey()));
        ECKey buyerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getBuyerEscrowPubKey()));

        Maybe<TransactionSignature> signature = getPayoutSignature(payoutAmount, fundingTx,
                arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey,
                payoutAddress);

        return signature.map(sig -> Base58.encode(sig.encodeToBitcoin()));
    }

    private Maybe<TransactionSignature> getPayoutSignature(Coin payoutAmount,
                                                           Transaction fundingTx,
                                                           ECKey arbitratorProfilePubKey,
                                                           ECKey sellerEscrowPubKey,
                                                           ECKey buyerEscrowPubKey,
                                                           Address payoutAddress) {

        return getTradeWallet().map(tw -> {
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

    // TODO make sure trades always have funding tx with amount added when loaded
    // TODO handle InsufficientMoneyException

    public Maybe<String> payoutEscrowToBuyer(Trade trade) {

        Address buyerPayoutAddress = Address.fromBase58(netParams, trade.getBuyerPayoutAddress());

        Maybe<String> signature = getPayoutSignature(trade, trade.fundingTransactionWithAmt().getTransaction(), buyerPayoutAddress);

        Maybe<TransactionSignature> mySignature = signature.map(s -> TransactionSignature
                .decodeFromBitcoin(Base58.decode(s), true, true));

        Maybe<TransactionSignature> buyerSignature = Maybe.just(TransactionSignature
                .decodeFromBitcoin(Base58.decode(trade.getPayoutTxSignature()), true, true));

        Single<List<TransactionSignature>> signatures = mySignature.concatWith(buyerSignature).toList();

        return signatures.flatMapMaybe(sl -> payoutEscrow(trade, buyerPayoutAddress, sl));
    }

    public Maybe<String> refundEscrowToSeller(Trade trade) {

        Address sellerRefundAddress = Address.fromBase58(netParams, trade.getRefundAddress());

        Maybe<String> signature = getPayoutSignature(trade, trade.fundingTransactionWithAmt().getTransaction(), sellerRefundAddress);

        Maybe<TransactionSignature> mySignature = signature.map(s -> TransactionSignature
                .decodeFromBitcoin(Base58.decode(s), true, true));

        Maybe<TransactionSignature> sellerSignature = Maybe.just(TransactionSignature
                .decodeFromBitcoin(Base58.decode(trade.getRefundTxSignature()), true, true));

        Single<List<TransactionSignature>> signatures = mySignature.concatWith(sellerSignature).toList();

        return signatures.flatMapMaybe(sl -> payoutEscrow(trade, sellerRefundAddress, sl));
    }

    public Maybe<String> cancelEscrowToSeller(Trade trade) {

        Address sellerRefundAddress = Address.fromBase58(netParams, trade.getRefundAddress());

        Maybe<String> signature = getPayoutSignature(trade, trade.fundingTransactionWithAmt().getTransaction(), sellerRefundAddress);

        Maybe<TransactionSignature> mySignature = signature.map(s -> TransactionSignature
                .decodeFromBitcoin(Base58.decode(s), true, true));

        Maybe<TransactionSignature> sellerSignature = Maybe.just(TransactionSignature
                .decodeFromBitcoin(Base58.decode(trade.getRefundTxSignature()), true, true));

        Single<List<TransactionSignature>> signatures = sellerSignature.concatWith(mySignature).toList();

        return signatures.flatMapMaybe(sl -> payoutEscrow(trade, sellerRefundAddress, sl));
    }

    private Maybe<String> payoutEscrow(Trade trade, Address payoutAddress,
                                       List<TransactionSignature> signatures) {

        Transaction fundingTx = trade.fundingTransactionWithAmt().getTransaction();

        Transaction payoutTx = new Transaction(netParams);
        payoutTx.setPurpose(Transaction.Purpose.ASSURANCE_CONTRACT_CLAIM);
        Coin payoutAmount = Coin.parseCoin(trade.getBtcAmount().toPlainString());

        ECKey arbitratorProfilePubKey = ECKey.fromPublicOnly(Base58.decode(trade.getArbitratorProfilePubKey()));
        ECKey sellerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getSellerEscrowPubKey()));
        ECKey buyerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getBuyerEscrowPubKey()));

        Script redeemScript = redeemScript(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey);
        Address escrowAddress = escrowAddress(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey);

        // add input to payout tx from single matching funding tx output
        for (TransactionOutput txo : fundingTx.getOutputs()) {
            Address outputAddress = txo.getAddressFromP2SH(netParams);
            Coin outputAmount = payoutAmount.plus(defaultTxFeeCoin());

            // verify output from fundingTx exists and equals required payout amounts
            if (outputAddress != null && outputAddress.equals(escrowAddress)
                    && txo.getValue().equals(outputAmount)) {

                // post payout input and funding output with signed unlock script
                TransactionInput input = payoutTx.addInput(txo);
                Script signedUnlockScript = ScriptBuilder.createP2SHMultiSigInputScript(signatures, redeemScript);
                input.setScriptSig(signedUnlockScript);
                break;
            }
        }

        // add output to payout tx
        payoutTx.addOutput(payoutAmount, payoutAddress);

        log.debug("Validate inputs for payoutTx: {}", payoutTx.toString());
        for (TransactionInput input : payoutTx.getInputs()) {
            log.debug("Validating input for payoutTx: {}", input.toString());
            try {
                input.verify(input.getConnectedOutput());
                log.debug("Input valid for payoutTx: {}", input.toString());
            } catch (VerificationException ve) {
                log.error("Input not valid for payoutTx, {}", ve.getMessage());
            } catch (NullPointerException npe) {
                log.error("Null connectedOutput for payoutTx");
            }
        }

        return getEscrowWallet(trade.getEscrowAddress())
                .zipWith(getPeerGroup(), (ew, pg) -> {
                    Context.propagate(btcContext);
                    //ew.commitTx(payoutTx);
                    pg.broadcastTransaction(payoutTx);
                    return payoutTx.getHash().toString();
                });
    }

    private String getSeedWords(Wallet wallet) {
        return Joiner.on(" ").join(wallet.getKeyChainSeed().getMnemonicCode());
    }

    private String getXprvKey(Wallet wallet) {
        return wallet.getWatchingKey().serializePrivB58(netParams);
    }

    private String getXpubKey(Wallet wallet) {
        return wallet.getWatchingKey().serializePubB58(netParams);
    }

    public class TradeWalletInfo {

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
}
