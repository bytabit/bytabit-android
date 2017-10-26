package com.bytabit.mobile.wallet;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.nav.evt.QuitEvent;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.evt.*;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.bytabit.mobile.wallet.model.TransactionWithAmtBuilder;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.*;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.BackpressureOverflow;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nullable;
import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.bytabit.mobile.trade.TradeManager.TRADES_PATH;
import static org.bitcoinj.core.Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;

public class WalletManager {

    private final static String TRADE_WALLET_FILE_NAME = "trade.wallet";
    private final static String ESCROW_WALLET_FILE_NAME = "escrow.wallet";
    private final static String BACKUP_EXT = ".bkp";
    private final static String SAVE_EXT = ".sav";

    private final static Logger LOG = LoggerFactory.getLogger(WalletManager.class);
    private final static NetworkParameters netParams;

    private final Context btcContext;

    private PeerGroup peerGroup;
    private BlockChain blockChain;
    private Wallet tradeWallet;

    private final Map<String, Wallet> escrowWallets = new HashMap<>();
    private final File blockStoreFile;

    // UI bindable properties
    private final DoubleProperty downloadProgress;
    private final StringProperty tradeWalletBalance;
    private final ObservableList<TransactionWithAmt> tradeWalletTransactions;
    private final BooleanProperty started;

    // rxJava observables
    private final Observable<BlockDownloadEvent> blockDownloadEvents;
    private final Observable<TransactionUpdatedEvent> tradeTxUpdatedEvents;
    private final Map<String, Observable<TransactionUpdatedEvent>> escrowTxUpdatedEvents = new HashMap<>();

    private Subscription blockDownloadSubscription;

    static {
        netParams = NetworkParameters.fromID("org.bitcoin." + AppConfig.getBtcNetwork());
    }

    public WalletManager() {

        this.tradeWalletTransactions = FXCollections.observableArrayList();
        this.tradeWalletBalance = new SimpleStringProperty();
        this.downloadProgress = new SimpleDoubleProperty();
        this.started = new SimpleBooleanProperty();
        started.setValue(false);

        btcContext = Context.getOrCreate(netParams);
        Context.propagate(btcContext);

        blockStoreFile = new File(AppConfig.getPrivateStorage(), "bytabit.spvchain");
        Boolean blockStoreFileExists = blockStoreFile.exists();
        tradeWallet = createOrLoadWallet(TRADE_WALLET_FILE_NAME, blockStoreFile, blockStoreFileExists);

        // post observable download events
        blockDownloadEvents = Observable.create((Observable.OnSubscribe<BlockDownloadEvent>) subscriber -> {

            peerGroup = createPeerGroup(blockStoreFile, blockStoreFileExists);
            peerGroup.start();
            peerGroup.startBlockChainDownload(new DownloadProgressTracker() {
                @Override
                public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {
                    super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft);
                    subscriber.onNext(new BlockDownloadedBuilder().peer(peer).block(block).filteredBlock(filteredBlock).blocksLeft(blocksLeft).build());
                }

                @Override
                protected void progress(double pct, int blocksSoFar, Date date) {
                    super.progress(pct, blocksSoFar, date);
                    subscriber.onNext(new BlockDownloadProgressBuilder().pct(pct / 100.00).blocksSoFar(blocksSoFar).date(LocalDateTime.fromDateFields(date)).build());
                }

                @Override
                protected void doneDownload() {
                    super.doneDownload();
                    subscriber.onNext(new BlockDownloadDone());
                }
            });
            // on un-subscribe stop peer group
            subscriber.add(Subscriptions.create(() -> {
                peerGroup.stop();
            }));
        }).onBackpressureBuffer(100, null, BackpressureOverflow.ON_OVERFLOW_DROP_OLDEST).share();

        // post observable wallet running events
        tradeTxUpdatedEvents = Observable.create((Observable.OnSubscribe<TransactionUpdatedEvent>) subscriber -> {

            TransactionConfidenceEventListener listener = new TransactionConfidenceEventListener() {

                @Override
                public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                    Context.propagate(btcContext);
                    subscriber.onNext(new TransactionUpdatedEventBuilder().tx(tx).wallet(wallet).build());
                }
            };
            tradeWallet.addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);

            subscriber.add(Subscriptions.create(() -> tradeWallet.removeTransactionConfidenceEventListener(listener)));
        }).onBackpressureBuffer(100, null, BackpressureOverflow.ON_OVERFLOW_DROP_OLDEST).share();

        File tradesDir = new File(TRADES_PATH);
        List<String> escrowAddresses;
        if (tradesDir.list() != null) {
            escrowAddresses = Arrays.asList(tradesDir.list());
        } else {
            escrowAddresses = new ArrayList<>();
        }

        for (String escrowAddress : escrowAddresses) {
            final File escrowWalletFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME);
            final File escrowWalletBackupFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME + BACKUP_EXT);
            if (escrowWalletFile.exists()) {
                escrowWallets.put(escrowAddress, createOrLoadWallet(escrowWalletFile, escrowWalletBackupFile, blockStoreFile, blockStoreFileExists));
            }
        }

        // post observable wallet running events
        for (String escrowAddress : escrowWallets.keySet()) {
            escrowTxUpdatedEvents.put(escrowAddress, Observable.create((Observable.OnSubscribe<TransactionUpdatedEvent>) subscriber -> {
                TransactionConfidenceEventListener listener = new TransactionConfidenceEventListener() {

                    @Override
                    public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                        Context.propagate(btcContext);
                        subscriber.onNext(new TransactionUpdatedEventBuilder().tx(tx).wallet(wallet).build());
                    }
                };
                escrowWallets.get(escrowAddress).addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);

                subscriber.add(Subscriptions.create(() -> escrowWallets.get(escrowAddress).removeTransactionConfidenceEventListener(listener)));
            }).onBackpressureBuffer(100, null, BackpressureOverflow.ON_OVERFLOW_DROP_OLDEST).share());
        }

        BytabitMobile.getNavEvents().filter(ne -> ne instanceof QuitEvent).subscribe(qe -> {
            LOG.debug("Got quit event");
            Context.propagate(btcContext);
            tradeWallet.shutdownAutosaveAndWait();
            for (Wallet escrowWallet : escrowWallets.values()) {
                escrowWallet.shutdownAutosaveAndWait();
            }
            LOG.debug("Shutdown wallets autosave.");
        });
    }

    public void start() {

        if (!started.get()) {
            LOG.debug("Wallet starting.");

            started.setValue(true);

            // if blockstore file removed, reset wallets
            if (!blockStoreFile.exists()) {
                LOG.debug("No blockstore file, resetting wallets.");
                tradeWallet.reset();
                for (Wallet escrowWallet : escrowWallets.values()) {
                    escrowWallet.reset();
                }
            }

            // get existing tx
            Set<TransactionWithAmt> txsWithAmt = new HashSet<>();
            for (Transaction t : tradeWallet.getTransactions(false)) {
                txsWithAmt.add(new TransactionWithAmtBuilder().tx(t).coinAmt(t.getValue(tradeWallet)).outputAddress(getWatchedOutputAddress(t)).inputTxHash(t.getInput(0).getOutpoint().getHash().toString()).build());
            }

            javafx.application.Platform.runLater(() -> {
                tradeWalletTransactions.addAll(txsWithAmt);
                tradeWalletBalance.setValue(tradeWallet.getBalance().toFriendlyString());
            });

            // listen for other events

            blockDownloadSubscription = blockDownloadEvents.observeOn(Schedulers.io())
                    .subscribe(e -> {
                        javafx.application.Platform.runLater(() -> {
                            if (e instanceof BlockDownloadDone) {
                                BlockDownloadDone dde = BlockDownloadDone.class.cast(e);
                                downloadProgress.setValue(1.0);
                            } else if (e instanceof BlockDownloadProgress) {
                                BlockDownloadProgress dpe = BlockDownloadProgress.class.cast(e);
                                downloadProgress.setValue(dpe.getPct());
                            }
                            tradeWalletBalance.setValue(getWalletBalance().toFriendlyString());
                        });
                    });

            tradeTxUpdatedEvents.observeOn(Schedulers.io())
                    .subscribe(e -> {
                        //LOG.debug("trade transaction updated event : {}", e);
                        TransactionUpdatedEvent txe = TransactionUpdatedEvent.class.cast(e);

                        TransactionWithAmt txu = new TransactionWithAmtBuilder().tx(txe.getTx()).coinAmt(txe.getAmt()).outputAddress(getWatchedOutputAddress(txe.getTx())).inputTxHash(txe.getTx().getInput(0).getOutpoint().getHash().toString()).build();

                        javafx.application.Platform.runLater(() -> {
                            Integer index = tradeWalletTransactions.indexOf(txu);
                            if (index > -1) {
                                tradeWalletTransactions.set(index, txu);
                            } else {
                                tradeWalletTransactions.add(txu);
                            }
                            tradeWalletBalance.setValue(getWalletBalance().toFriendlyString());
                        });
                    });

            for (String escrowAddress : escrowTxUpdatedEvents.keySet()) {
                escrowTxUpdatedEvents.get(escrowAddress).observeOn(Schedulers.io())
                        .subscribe(e -> {
                            LOG.debug("escrow {} transaction updated event : {}", escrowAddress, e);
                        });
            }
            LOG.debug("Wallet started.");
        }
    }

    private PeerGroup createPeerGroup(File blockStoreFile, Boolean blockStoreFileExists) {
        try {
            BlockStore blockStore = new SPVBlockStore(netParams, blockStoreFile);
            blockStore.getChainHead(); // detect corruptions as early as possible

            final long earliestKeyCreationTime = tradeWallet.getEarliestKeyCreationTime();

            if (!blockStoreFileExists && earliestKeyCreationTime > 0) {
                try {
                    final InputStream checkpointsInputStream = WalletManager.class.getClassLoader().getResourceAsStream(getNetParams().getId() + ".checkpoints.txt");
                    CheckpointManager.checkpoint(getNetParams(), checkpointsInputStream, blockStore, earliestKeyCreationTime);
                } catch (final IOException x) {
                    LOG.error("problem reading checkpoints, continuing without", x);
                }
            }

            List<Wallet> wallets = new ArrayList<>();
            wallets.add(tradeWallet);
            wallets.addAll(escrowWallets.values());
            blockChain = new BlockChain(netParams, wallets, blockStore);
            PeerGroup peerGroup = new PeerGroup(netParams, blockChain);
            peerGroup.setUserAgent("org.bytabit.mobile", AppConfig.getVersion());

            peerGroup.addWallet(tradeWallet);
            for (Wallet escrowWallet : escrowWallets.values()) {
                peerGroup.addWallet(escrowWallet);
            }

            if (netParams.equals(RegTestParams.get())) {
                peerGroup.setMaxConnections(1);
                peerGroup.addAddress(new PeerAddress(netParams, InetAddress.getLocalHost(), netParams.getPort()));

            } else {
                peerGroup.addPeerDiscovery(new DnsDiscovery(netParams));
            }
            peerGroup.setFastCatchupTimeSecs(earliestKeyCreationTime);
            return peerGroup;

        } catch (BlockStoreException bse) {
            LOG.error("Can't open block store.");
            throw new RuntimeException(bse);
        } catch (UnknownHostException uhe) {
            LOG.error("Can't add localhost to peer group.");
            throw new RuntimeException(uhe);
        }
    }

    public void resetBlockchain() {
        blockDownloadSubscription.unsubscribe();
        File blockStoreFile = new File(AppConfig.getPrivateStorage(), "bytabit.spvchain");
        Boolean blockStoreFileExists = blockStoreFile.exists();
        if (blockStoreFile.delete()) {
            peerGroup = createPeerGroup(blockStoreFile, blockStoreFileExists);
            tradeWallet.reset();
            for (Wallet escrowWallet : escrowWallets.values()) {
                escrowWallet.reset();
            }
            blockDownloadSubscription = blockDownloadEvents.observeOn(Schedulers.io())
                    .subscribe(e -> {
                        javafx.application.Platform.runLater(() -> {
                            if (e instanceof BlockDownloadDone) {
                                BlockDownloadDone dde = BlockDownloadDone.class.cast(e);
                                downloadProgress.setValue(1.0);
                            } else if (e instanceof BlockDownloadProgress) {
                                BlockDownloadProgress dpe = BlockDownloadProgress.class.cast(e);
                                downloadProgress.setValue(dpe.getPct());
                            }
                            tradeWalletBalance.setValue(getWalletBalance().toFriendlyString());
                        });
                    });
        }
    }

    private Wallet createOrLoadWallet(String walletFileName, File blockStoreFile, Boolean blockStoreFileExists) {
        final File walletFile = new File(AppConfig.getPrivateStorage(), walletFileName);
        final File walletBackupFile = new File(AppConfig.getPrivateStorage(), walletFileName + BACKUP_EXT);
        return createOrLoadWallet(walletFile, walletBackupFile, blockStoreFile, blockStoreFileExists);
    }

    private Wallet createOrLoadWallet(File walletFile, File walletBackupFile, File blockStoreFile, Boolean blockStoreFileExists) {

        Wallet wallet;
        if (walletFile.exists()) {
            try {
                wallet = loadWallet(walletFile);
            } catch (FileNotFoundException | UnreadableWalletException e) {
                try {
                    wallet = loadWallet(walletBackupFile);
                    // have to remove blockstore file so wallet will be reloaded
                    if (blockStoreFileExists) {
                        LOG.debug("Removed blockstore file: {}", blockStoreFile.getName());
                        blockStoreFile.delete();
                    }
                } catch (FileNotFoundException | UnreadableWalletException e1) {
                    LOG.error("Unable to load backup wallet: {}", walletBackupFile.getName());
                    throw new RuntimeException(e1);
                }
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

        Wallet wallet;
        FileInputStream walletInputStream = null;
        try {
            walletInputStream = new FileInputStream(walletFile);
            final WalletProtobufSerializer serializer = new WalletProtobufSerializer();
            wallet = serializer.readWallet(walletInputStream);
            if (!wallet.getParams().equals(netParams)) {
                LOG.error("Invalid network params in wallet file: {}", walletFile.toString());
                throw new UnreadableWalletException("Wrong network parameters, found: " + wallet.getParams().getId() + " should be: " + netParams.getId());
            }
            LOG.debug("Loaded wallet file: {}", walletFile.getName());
        } catch (FileNotFoundException e) {
            LOG.error("Wallet file does not exist: {}", walletFile.toString());
            throw e;

        } catch (UnreadableWalletException e) {
            LOG.error("Wallet file is unreadable: {}", walletFile.toString());
            throw e;
        } finally {
            if (walletInputStream != null) {
                try {
                    walletInputStream.close();
                } catch (final IOException x) {
                    LOG.error("Unable to close wallet input stream: {}", walletFile.toString());
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
            LOG.info("Wallet saved to: {}", walletFile.getName());
        } catch (final IOException e) {
            LOG.error("Can't save wallet", e);
        } finally {
            if (walletOutputStream != null) {
                try {
                    walletOutputStream.close();
                } catch (final IOException x) {
                    LOG.error("Unable to close wallet output stream: {}", walletFile.getName());
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
            LOG.info("Wallet backed up to: {}", walletBackupFile.getName());
        } catch (final IOException e) {
            LOG.error("Can't save wallet backup", e);
        } finally {
            if (walletOutputStream != null) {
                try {
                    walletOutputStream.close();
                } catch (final IOException x) {
                    LOG.error("Unable to close wallet backup output stream: {}", walletBackupFile.getName());
                }
            }
        }
        LOG.debug("Backed up wallet to file: {}", walletBackupFile.getName());
    }

    public DoubleProperty downloadProgressProperty() {
        return downloadProgress;
    }

    public ObservableList<TransactionWithAmt> getTradeWalletTransactions() {
        return tradeWalletTransactions;
    }

    public StringProperty getTradeWalletBalance() {
        return tradeWalletBalance;
    }

    public NetworkParameters getNetParams() {
        return netParams;
    }

    public Address getDepositAddress() {
        return tradeWallet.currentReceiveAddress();
    }

    public Address getFreshReceiveAddress() {
        return tradeWallet.freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).toAddress(netParams);
    }

    public String getFreshBase58AuthPubKey() {
        return Base58.encode(tradeWallet.freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).getPubKey());
    }

    public Coin getWalletBalance() {
        return tradeWallet.getBalance();
    }

    public BigDecimal defaultTxFee() {
        return new BigDecimal(defaultTxFeeCoin().toPlainString());
    }

    private Coin defaultTxFeeCoin() {
        return REFERENCE_DEFAULT_MIN_TX_FEE;
    }

    public Transaction fundEscrow(String escrowAddress, BigDecimal amount) throws InsufficientMoneyException {
        // TODO determine correct amount for extra tx fee for payout, current using DEFAULT_TX_FEE
        SendRequest sendRequest = SendRequest.to(Address.fromBase58(netParams, escrowAddress),
                Coin.parseCoin(amount.toString()).add(defaultTxFeeCoin()));
        Transaction tx = tradeWallet.sendCoins(sendRequest).tx;
        return tx;
    }

    private static Address escrowAddress(ECKey arbitratorProfilePubKey,
                                         ECKey sellerEscrowPubKey,
                                         ECKey buyerEscrowPubKey) {
        return ScriptBuilder.createP2SHOutputScript(redeemScript(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey)).getToAddress(netParams);
    }

    public static String escrowAddress(String arbitratorProfilePubKey,
                                       String sellerEscrowPubKey,
                                       String buyerEscrowPubKey) {
        ECKey apk = ECKey.fromPublicOnly(Base58.decode(arbitratorProfilePubKey));
        ECKey spk = ECKey.fromPublicOnly(Base58.decode(sellerEscrowPubKey));
        ECKey bpk = ECKey.fromPublicOnly(Base58.decode(buyerEscrowPubKey));
        return escrowAddress(apk, spk, bpk).toBase58();
    }

    private ECKey getECKeyFromAddress(Address address) {
        return tradeWallet.findKeyFromPubHash(address.getHash160());
    }

    private String getWatchedOutputAddress(Transaction tx) {
        List<String> watchedOutputAddresses = new ArrayList<>();
        for (TransactionOutput output : tx.getOutputs()) {
            Script script = new Script(output.getScriptBytes());
            Address address = output.getAddressFromP2PKHScript(netParams);
            if (address != null && tradeWallet.isWatchedScript(script)) {
                watchedOutputAddresses.add(address.toBase58());
            } else {
                address = output.getAddressFromP2SH(netParams);
                if (address != null && tradeWallet.isWatchedScript(script)) {
                    watchedOutputAddresses.add(address.toBase58());
                }
            }
        }

        if (watchedOutputAddresses.size() > 1) {
            LOG.error("Found more than one watched output address.");
        }
        return watchedOutputAddresses.size() > 0 ? watchedOutputAddresses.get(0) : null;
    }

    public TransactionWithAmt getTransactionWithAmt(String escrowAddress, String txHash, String toAddress) {
        TransactionWithAmt transactionWithAmt = null;
        Sha256Hash hash = Sha256Hash.wrap(txHash);
        Address addr = Address.fromBase58(netParams, toAddress);
        Transaction tx = escrowWallets.get(escrowAddress).getTransaction(hash);
        if (tx == null) {
            tx = tradeWallet.getTransaction(hash);
        }
        if (tx != null) {
            Coin amt = Coin.ZERO;
            for (TransactionOutput output : tx.getOutputs()) {
                if (!addr.isP2SHAddress() && addr.equals(output.getAddressFromP2PKHScript(netParams))
                        || addr.isP2SHAddress() && addr.equals(output.getAddressFromP2SH(netParams))) {
                    amt = amt.add(output.getValue());
                }
            }
            transactionWithAmt = new TransactionWithAmtBuilder().tx(tx).coinAmt(amt).outputAddress(toAddress).inputTxHash(null).build();
        } else {
            LOG.error("Can't find Tx with hash: {} to address {}:", txHash, toAddress);
        }

        return transactionWithAmt;
    }

    public TransactionWithAmt getEscrowTransactionWithAmt(String escrowAddress, String txHash) {
        Transaction tx = getEscrowTransaction(escrowAddress, txHash);
        if (tx != null) {
            TransactionUpdatedEvent txe = TransactionUpdatedEvent.builder().tx(tx).wallet(escrowWallets.get(escrowAddress)).build();
            return new TransactionWithAmtBuilder().tx(tx).coinAmt(txe.getAmt()).outputAddress(getWatchedOutputAddress(tx)).inputTxHash(tx.getInput(0).getOutpoint().getHash().toString()).build();
        } else {
            return null;
        }
    }

    public TransactionWithAmt getTradeTransactionWithAmt(String txHash) {
        Transaction tx = getTradeTransaction(txHash);
        if (tx != null) {
            TransactionUpdatedEvent txe = new TransactionUpdatedEventBuilder().tx(tx).wallet(tradeWallet).build();
            return new TransactionWithAmtBuilder().tx(tx).coinAmt(txe.getAmt()).outputAddress(getWatchedOutputAddress(tx)).inputTxHash(tx.getInput(0).getOutpoint().getHash().toString()).build();
        } else return null;
    }

    public Transaction getEscrowTransaction(String escrowAddress, String txHash) {
        Sha256Hash hash = Sha256Hash.wrap(txHash);
        Transaction tx = escrowWallets.get(escrowAddress).getTransaction(hash);
        if (tx == null) {
            //throw new RuntimeException("Can't find escrow Tx with hash: " + txHash);
            LOG.error("Can't find escrow Tx with hash to: {}", txHash);
        }
        return tx;
    }

    public Transaction getTradeTransaction(String txHash) {
        Sha256Hash hash = Sha256Hash.wrap(txHash);
        Transaction tx = tradeWallet.getTransaction(hash);
        if (tx == null) {
            //throw new RuntimeException("Can't find escrow Tx with hash: " + txHash);
            LOG.error("Can't find trade Tx with hash: {}", txHash);
        }
        return tx;
    }

    public String getPayoutSignature(Trade trade, Transaction fundingTx) {
        Address buyerPayoutAddress = Address.fromBase58(netParams, trade.getBuyerPayoutAddress());
        return getPayoutSignature(trade, fundingTx, buyerPayoutAddress);
    }

    public String getRefundSignature(Trade trade, Transaction fundingTx, Address sellerRefundAddress) {
        return getPayoutSignature(trade, fundingTx, sellerRefundAddress);
    }

    public String getPayoutSignature(Trade trade, Transaction fundingTx, Address payoutAddress) {
        Coin payoutAmount = Coin.parseCoin(trade.getBtcAmount().toPlainString());
        ECKey arbitratorProfilePubKey = ECKey.fromPublicOnly(Base58.decode(trade.getArbitratorProfilePubKey()));
        ECKey sellerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getSellerEscrowPubKey()));
        ECKey buyerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getBuyerEscrowPubKey()));

        TransactionSignature signature = getPayoutSignature(payoutAmount, fundingTx,
                arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey,
                payoutAddress);

        return Base58.encode(signature.encodeToBitcoin());
    }

    private TransactionSignature getPayoutSignature(Coin payoutAmount,
                                                    Transaction fundingTx,
                                                    ECKey arbitratorProfilePubKey,
                                                    ECKey sellerEscrowPubKey,
                                                    ECKey buyerEscrowPubKey,
                                                    Address payoutAddress) {

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
        ECKey escrowKey = tradeWallet.findKeyFromPubKey(buyerEscrowPubKey.getPubKey());
        if (escrowKey == null) {
            escrowKey = tradeWallet.findKeyFromPubKey(sellerEscrowPubKey.getPubKey());
        }
        if (escrowKey == null) {
            escrowKey = tradeWallet.findKeyFromPubKey(arbitratorProfilePubKey.getPubKey());
        }
        if (escrowKey != null) {
            // sign tx input
            Sha256Hash unlockSigHash = payoutTx.hashForSignature(0, redeemScript, Transaction.SigHash.ALL, false);
            return new TransactionSignature(escrowKey.sign(unlockSigHash), Transaction.SigHash.ALL, false);
        } else {
            throw new RuntimeException("Can not create payout signature, no signing key found.");
        }
    }

    private static Script redeemScript(ECKey arbitratorProfilePubKey,
                                       ECKey sellerEscrowPubKey,
                                       ECKey buyerEscrowPubKey) {
        return ScriptBuilder.createMultiSigOutputScript(2, ImmutableList.of(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey));
    }

    public String payoutEscrowToBuyer(Trade trade) throws InsufficientMoneyException {

        Address buyerPayoutAddress = Address.fromBase58(netParams, trade.getBuyerPayoutAddress());

        String fundingTxHash = trade.getFundingTxHash();
        Transaction fundingTx = getEscrowTransaction(trade.getEscrowAddress(), fundingTxHash);

        String signature = getPayoutSignature(trade, fundingTx, buyerPayoutAddress);

        TransactionSignature sellerSignature = TransactionSignature
                .decodeFromBitcoin(Base58.decode(signature), true, true);

        TransactionSignature buyerSignature = TransactionSignature
                .decodeFromBitcoin(Base58.decode(trade.getPayoutTxSignature()), true, true);

        List<TransactionSignature> signatures = ImmutableList.of(sellerSignature, buyerSignature);

        return payoutEscrow(trade, buyerPayoutAddress, signatures);
    }

    public String refundEscrowToSeller(Trade trade) throws InsufficientMoneyException {

        Address sellerRefundAddress = Address.fromBase58(netParams, trade.getRefundAddress());

        String fundingTxHash = trade.getFundingTxHash();
        Transaction fundingTx = getEscrowTransaction(trade.getEscrowAddress(), fundingTxHash);

        String signature = getPayoutSignature(trade, fundingTx, sellerRefundAddress);

        TransactionSignature arbitratorSignature = TransactionSignature
                .decodeFromBitcoin(Base58.decode(signature), true, true);

        TransactionSignature sellerSignature = TransactionSignature
                .decodeFromBitcoin(Base58.decode(trade.getRefundTxSignature()), true, true);

        List<TransactionSignature> signatures = ImmutableList.of(arbitratorSignature, sellerSignature);

        return payoutEscrow(trade, sellerRefundAddress, signatures);
    }

    public String cancelEscrowToSeller(Trade trade) throws InsufficientMoneyException {

        Address sellerRefundAddress = Address.fromBase58(netParams, trade.getRefundAddress());

        String fundingTxHash = trade.getFundingTxHash();
        Transaction fundingTx = getEscrowTransaction(trade.getEscrowAddress(), fundingTxHash);

        String signature = getPayoutSignature(trade, fundingTx, sellerRefundAddress);

        TransactionSignature sellerSignature = TransactionSignature
                .decodeFromBitcoin(Base58.decode(trade.getRefundTxSignature()), true, true);

        TransactionSignature buyerSignature = TransactionSignature
                .decodeFromBitcoin(Base58.decode(signature), true, true);

        List<TransactionSignature> signatures = ImmutableList.of(sellerSignature, buyerSignature);

        return payoutEscrow(trade, sellerRefundAddress, signatures);
    }

    private String payoutEscrow(Trade trade, Address payoutAddress,
                                List<TransactionSignature> signatures) {

        String fundingTxHash = trade.getFundingTxHash();
        Transaction fundingTx = getEscrowTransaction(trade.getEscrowAddress(), fundingTxHash);

        if (fundingTx != null) {

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

            LOG.debug("Validate inputs for payoutTx: {}", payoutTx.toString());
            for (TransactionInput input : payoutTx.getInputs()) {
                LOG.debug("Validating input for payoutTx: {}", input.toString());
                try {
                    input.verify(input.getConnectedOutput());
                    LOG.debug("Input valid for payoutTx: {}", input.toString());
                } catch (VerificationException ve) {
                    LOG.error("Input not valid for payoutTx, {}", ve.getMessage());
                } catch (NullPointerException npe) {
                    LOG.error("Null connectedOutput for payoutTx");
                }
            }

            escrowWallets.get(trade.getEscrowAddress()).commitTx(payoutTx);
            peerGroup.broadcastTransaction(payoutTx);
            return payoutTx.getHash().toString();
        } else {
            // TODO reset blockchain and reload, then retry...
            LOG.error("No funding tx found for payout tx.");
            throw new RuntimeException("No funding tx found for payout tx.");
        }
    }

    public String sendCoins(SendRequest sendRequest) throws InsufficientMoneyException {
        Transaction tx = tradeWallet.sendCoins(sendRequest).tx;
        return tx.getHashAsString();
    }

    public void createEscrowWallet(String escrowAddress) {
        Context.propagate(btcContext);
        Address address = Address.fromBase58(getNetParams(), escrowAddress);
        if (!escrowWallets.containsKey(escrowAddress)) {

            File tradeDir = new File(TRADES_PATH + escrowAddress);
            if (!tradeDir.exists()) {
                tradeDir.mkdirs();
                LOG.debug("Created new trade dir: {}", tradeDir);
            }
            final File escrowWalletFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME);
            final File escrowWalletBackupFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME + BACKUP_EXT);
            final File blockStoreFile = new File(AppConfig.getPrivateStorage(), "bytabit.spvchain");

            Wallet escrowWallet = createOrLoadWallet(escrowWalletFile, escrowWalletBackupFile, blockStoreFile, true);
            escrowWallet.addWatchedAddress(address, (DateTime.now().getMillis() / 1000));
            escrowWallets.put(escrowAddress, escrowWallet);

            Observable<TransactionUpdatedEvent> escrowWalletTxObservable = Observable.create((Observable.OnSubscribe<TransactionUpdatedEvent>) subscriber -> {
                TransactionConfidenceEventListener listener = new TransactionConfidenceEventListener() {

                    @Override
                    public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                        Context.propagate(btcContext);
                        subscriber.onNext(new TransactionUpdatedEventBuilder().tx(tx).wallet(wallet).build());
                    }
                };
                escrowWallet.addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);

                subscriber.add(Subscriptions.create(() -> escrowWallet.removeTransactionConfidenceEventListener(listener)));
            }).onBackpressureBuffer(100, null, BackpressureOverflow.ON_OVERFLOW_DROP_OLDEST).share();

            escrowTxUpdatedEvents.put(escrowAddress, escrowWalletTxObservable);
            escrowWalletTxObservable.observeOn(Schedulers.io())
                    .subscribe(e -> {
                        LOG.debug("escrow {} transaction updated event : {}", escrowAddress, e);
                    });

            blockChain.addWallet(escrowWallet);
            peerGroup.addWallet(escrowWallet);
        }
//        if (escrowWallets.get(escrowAddress).addWatchedAddress(address, (DateTime.now().getMillis() / 1000))) {
//            LOG.debug("Added watched address: {}", address.toBase58());
//        } else {
//            LOG.warn("Failed to add watch address: {}", address.toBase58());
//        }
    }

    public void removeWatchedEscrowAddress(String escrowAddress) {
        Address address = Address.fromBase58(getNetParams(), escrowAddress);
        Context.propagate(btcContext);
        if (escrowWallets.containsKey(escrowAddress)) {
            escrowTxUpdatedEvents.get(escrowAddress).unsubscribeOn(Schedulers.io());
            blockChain.removeWallet(escrowWallets.get(escrowAddress));
            peerGroup.removeWallet(escrowWallets.get(escrowAddress));
            escrowWallets.get(escrowAddress).shutdownAutosaveAndWait();

            // rename wallet file
            final File escrowWalletFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME);
            final File escrowWalletSavFile = new File(TRADES_PATH + escrowAddress + File.separator + ESCROW_WALLET_FILE_NAME + SAVE_EXT);

            if (escrowWalletFile.renameTo(escrowWalletSavFile)) {
                if (escrowWalletFile.exists() && escrowWalletFile.delete()) {
                    LOG.debug("Deleted {}", escrowWalletFile);
                } else {
                    LOG.error("Failed to delete {}", escrowWalletFile);
                }
            } else {
                LOG.error("Failed to rename {} to {}", escrowWalletFile, escrowWalletSavFile);
            }

            LOG.debug("Removed watched escrow address: {}", address.toBase58());
        } else {
            LOG.warn("Failed to remove watched escrow address: {}", address.toBase58());
        }
    }

    public String getSeedWords() {
        return Joiner.on(" ").join(tradeWallet.getKeyChainSeed().getMnemonicCode());
    }

    public String getXprvKey() {
        return tradeWallet.getWatchingKey().serializePrivB58(netParams);
    }

    public String getXpubKey() {
        return tradeWallet.getWatchingKey().serializePubB58(netParams);
    }
}
