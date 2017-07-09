package com.bytabit.mobile.wallet;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.nav.evt.QuitEvent;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.evt.*;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.google.common.collect.ImmutableList;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import rx.schedulers.JavaFxScheduler;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.bitcoinj.core.Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;

public class WalletManager {

    private final static Logger LOG = LoggerFactory.getLogger(WalletManager.class);
    private final static NetworkParameters netParams;

    private final Context btcContext;

    private final PeerGroup peerGroup;
    private final Wallet tradeWallet;

    private final Wallet escrowWallet;

    private final StringProperty tradeWalletBalance;

    private final Observable<BlockDownloadEvent> blockDownloadEvents;
    private final DoubleProperty downloadProgress;

    private final Observable<TransactionUpdatedEvent> tradeTxUpdatedEvents;
    private final ObservableList<TransactionWithAmt> tradeWalletTransactions;
    private final Observable<TransactionUpdatedEvent> escrowTxUpdatedEvents;

    static {
        netParams = NetworkParameters.fromID("org.bitcoin." + AppConfig.getBtcNetwork());
    }

    public WalletManager() {

        this.tradeWalletTransactions = FXCollections.observableArrayList();
        this.tradeWalletBalance = new SimpleStringProperty();
        this.downloadProgress = new SimpleDoubleProperty();

        btcContext = Context.getOrCreate(netParams);
        Context.propagate(btcContext);

        tradeWallet = createOrLoadWallet(new File(AppConfig.getPrivateStorage(), "trade.wallet"));
        escrowWallet = createOrLoadWallet(new File(AppConfig.getPrivateStorage(), "escrow.wallet"));

        try {
            File blockStoreFile = new File(AppConfig.getPrivateStorage(), "bytabit.spvchain");
            BlockStore blockStore = new SPVBlockStore(netParams, blockStoreFile);
            BlockChain blockChain = new BlockChain(netParams, Arrays.asList(tradeWallet, escrowWallet), blockStore);
            peerGroup = new PeerGroup(netParams, blockChain);
            peerGroup.setUserAgent("org.bytabit.mobile", AppConfig.getVersion());

            peerGroup.addWallet(tradeWallet);
            peerGroup.addWallet(escrowWallet);

            if (netParams.equals(RegTestParams.get())) {
                peerGroup.setMaxConnections(1);
                peerGroup.addAddress(new PeerAddress(netParams, InetAddress.getLocalHost(), netParams.getPort()));

            } else {
                peerGroup.addPeerDiscovery(new DnsDiscovery(netParams));
            }

        } catch (BlockStoreException bse) {
            LOG.error("Can't open block store.");
            throw new RuntimeException(bse);
        } catch (UnknownHostException uhe) {
            LOG.error("Can't add localhost to peer group.");
            throw new RuntimeException(uhe);
        }

        BytabitMobile.getNavEvents().filter(ne -> ne instanceof QuitEvent).subscribe(qe -> {
            LOG.debug("Got quit event");
            Context.propagate(btcContext);
            tradeWallet.shutdownAutosaveAndWait();
            escrowWallet.shutdownAutosaveAndWait();
            LOG.debug("Shutdown and autosaved wallets.");
        });

        // post observable download events
        blockDownloadEvents = Observable.create((Observable.OnSubscribe<BlockDownloadEvent>) subscriber -> {
            peerGroup.start();
            peerGroup.startBlockChainDownload(new DownloadProgressTracker() {
                @Override
                public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {
                    super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft);
                    subscriber.onNext(new BlockDownloaded(peer, block, filteredBlock, blocksLeft));
                }

                @Override
                protected void progress(double pct, int blocksSoFar, Date date) {
                    super.progress(pct, blocksSoFar, date);
                    subscriber.onNext(new BlockDownloadProgress(pct / 100.00, blocksSoFar, LocalDateTime.fromDateFields(date)));
                }

                @Override
                protected void doneDownload() {
                    super.doneDownload();
                    subscriber.onNext(new BlockDownloadDone());
                }
            });
            // on un-subscribe stop peer group
            subscriber.add(Subscriptions.create(peerGroup::stop));
        }).onBackpressureBuffer(100, null, BackpressureOverflow.ON_OVERFLOW_DROP_OLDEST).share();

        // post observable wallet running events
        tradeTxUpdatedEvents = Observable.create((Observable.OnSubscribe<TransactionUpdatedEvent>) subscriber -> {
            TransactionConfidenceEventListener listener = new TransactionConfidenceEventListener() {

                @Override
                public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                    Context.propagate(btcContext);
                    subscriber.onNext(new TransactionUpdatedEvent(tx, wallet));
                }
            };
            tradeWallet.addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);

            subscriber.add(Subscriptions.create(() -> tradeWallet.removeTransactionConfidenceEventListener(listener)));
        }).onBackpressureBuffer(100, null, BackpressureOverflow.ON_OVERFLOW_DROP_OLDEST).share();

        // post observable wallet running events
        escrowTxUpdatedEvents = Observable.create((Observable.OnSubscribe<TransactionUpdatedEvent>) subscriber -> {
            TransactionConfidenceEventListener listener = new TransactionConfidenceEventListener() {

                @Override
                public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                    Context.propagate(btcContext);
                    subscriber.onNext(new TransactionUpdatedEvent(tx, wallet));
                }
            };
            escrowWallet.addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);

            subscriber.add(Subscriptions.create(() -> escrowWallet.removeTransactionConfidenceEventListener(listener)));
        }).onBackpressureBuffer(100, null, BackpressureOverflow.ON_OVERFLOW_DROP_OLDEST).share();

        // get existing tx
        Set<TransactionWithAmt> txsWithAmt = new HashSet<>();
        for (Transaction t : tradeWallet.getTransactions(false)) {
            txsWithAmt.add(new TransactionWithAmt(t, t.getValue(tradeWallet),
                    getWatchedOutputAddress(t), t.getInput(0).getOutpoint().getHash().toString()));
        }
        Platform.runLater(() -> {
            tradeWalletTransactions.addAll(txsWithAmt);
            tradeWalletBalance.setValue(tradeWallet.getBalance().toFriendlyString());
        });

        // listen for other events
        tradeTxUpdatedEvents.observeOn(JavaFxScheduler.getInstance())
                .subscribe(e -> {
                    //LOG.debug("trade transaction updated event : {}", e);
                    TransactionUpdatedEvent txe = TransactionUpdatedEvent.class.cast(e);

                    TransactionWithAmt txu = new TransactionWithAmt(txe.getTx(),
                            txe.getAmt(), getWatchedOutputAddress(txe.getTx()),
                            txe.getTx().getInput(0).getOutpoint().getHash().toString());

                    Integer index = tradeWalletTransactions.indexOf(txu);
                    if (index > -1) {
                        tradeWalletTransactions.set(index, txu);
                    } else {
                        tradeWalletTransactions.add(txu);
                    }
                    tradeWalletBalance.setValue(getWalletBalance().toFriendlyString());
                });

        blockDownloadEvents.observeOn(JavaFxScheduler.getInstance())
                .subscribe(e -> {
                    //LOG.debug("block download event : {}", e);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if (e instanceof BlockDownloadDone) {
                                BlockDownloadDone dde = BlockDownloadDone.class.cast(e);
                                downloadProgress.setValue(1.0);
                            } else if (e instanceof BlockDownloadProgress) {
                                BlockDownloadProgress dpe = BlockDownloadProgress.class.cast(e);
                                downloadProgress.setValue(dpe.getPct());
                            }
                            tradeWalletBalance.setValue(getWalletBalance().toFriendlyString());
                        }
                    });
                });

        escrowTxUpdatedEvents.observeOn(JavaFxScheduler.getInstance())
                .subscribe(e -> {
                    // LOG.debug("escrow transaction updated event : {}", e);
                });
    }

    private Wallet createOrLoadWallet(File walletFile) {
        Wallet wallet;
        if (walletFile.exists()) {
            try {
                FileInputStream walletStream = new FileInputStream(walletFile);
                try {
                    Protos.Wallet proto = WalletProtobufSerializer.parseToProto(walletStream);
                    final WalletProtobufSerializer serializer = new WalletProtobufSerializer();
                    WalletExtension[] extArray = new WalletExtension[0];
                    wallet = serializer.readWallet(netParams, extArray, proto);
                } finally {
                    walletStream.close();
                }
            } catch (FileNotFoundException fnfe) {
                LOG.error("Wallet file does not exist: " + walletFile.toString());
                throw new RuntimeException(fnfe);
            } catch (IOException | UnreadableWalletException irw) {
                LOG.error("Could not parse wallet file: " + walletFile.toString());
                throw new RuntimeException(irw);
            }
        } else {
            wallet = new Wallet(netParams);
        }
        wallet.autosaveToFile(walletFile, 10, TimeUnit.SECONDS, null);
        return wallet;
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

    public String fundEscrow(String escrowAddress, BigDecimal amount) throws InsufficientMoneyException {
        // TODO determine correct amount for extra tx fee for payout, current using DEFAULT_TX_FEE
        SendRequest sendRequest = SendRequest.to(Address.fromBase58(netParams, escrowAddress),
                Coin.parseCoin(amount.toString()).add(defaultTxFeeCoin()));
        Transaction tx = tradeWallet.sendCoins(sendRequest).tx;
        return tx.getHashAsString();
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

    public TransactionWithAmt getEscrowTransactionWithAmt(String txHash) {
        Transaction tx = getEscrowTransaction(txHash);
        if (tx != null) {
            TransactionUpdatedEvent txe = new TransactionUpdatedEvent(tx, escrowWallet);
            return new TransactionWithAmt(tx,
                    txe.getAmt(), getWatchedOutputAddress(tx),
                    tx.getInput(0).getOutpoint().getHash().toString());
        } else {
            return null;
        }
    }

    public TransactionWithAmt getTradeTransactionWithAmt(String txHash) {
        Transaction tx = getTradeTransaction(txHash);
        if (tx != null) {
            TransactionUpdatedEvent txe = new TransactionUpdatedEvent(tx, tradeWallet);
            return new TransactionWithAmt(tx,
                    txe.getAmt(), getWatchedOutputAddress(tx),
                    tx.getInput(0).getOutpoint().getHash().toString());
        } else return null;
    }

    public Transaction getEscrowTransaction(String txHash) {
        Sha256Hash hash = Sha256Hash.wrap(txHash);
        Transaction tx = escrowWallet.getTransaction(hash);
        if (tx == null) {
            //throw new RuntimeException("Can't find escrow Tx with hash: " + txHash);
            LOG.error("Can't find escrow Tx with hash: " + txHash);
        }
        return tx;
    }

    public Transaction getTradeTransaction(String txHash) {
        Sha256Hash hash = Sha256Hash.wrap(txHash);
        Transaction tx = tradeWallet.getTransaction(hash);
        if (tx == null) {
            //throw new RuntimeException("Can't find escrow Tx with hash: " + txHash);
            LOG.error("Can't find escrow Tx with hash: " + txHash);
        }
        return tx;
    }

    public String getPayoutSignature(Trade trade, Transaction fundingTx) {
        Coin payoutAmount = Coin.parseCoin(trade.getBuyRequest().getBtcAmount().toPlainString());
        ECKey arbitratorProfilePubKey = ECKey.fromPublicOnly(Base58.decode(trade.getSellOffer().getArbitratorProfilePubKey()));
        ECKey sellerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getSellOffer().getSellerEscrowPubKey()));
        ECKey buyerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getBuyRequest().getBuyerEscrowPubKey()));

        Address buyerPayoutAddress = Address.fromBase58(netParams, trade.getBuyRequest().getBuyerPayoutAddress());
        TransactionSignature signature = getPayoutSignature(payoutAmount, fundingTx,
                arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey,
                buyerPayoutAddress);

        return Base58.encode(signature.encodeToBitcoin());
    }

    private TransactionSignature getPayoutSignature(Coin payoutAmount,
                                                    Transaction fundingTx,
                                                    ECKey arbitratorProfilePubKey,
                                                    ECKey sellerEscrowPubKey,
                                                    ECKey buyerEscrowPubKey,
                                                    Address buyerPayoutAddress) {

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
        payoutTx.addOutput(payoutAmount, buyerPayoutAddress);

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
            throw new RuntimeException("Can post payout signature, no signing key found.");
        }
    }

    private static Script redeemScript(ECKey arbitratorProfilePubKey,
                                       ECKey sellerEscrowPubKey,
                                       ECKey buyerEscrowPubKey) {
        return ScriptBuilder.createMultiSigOutputScript(2, ImmutableList.of(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey));
    }

    public String payoutEscrow(Trade trade) throws InsufficientMoneyException {

        String fundingTxHash = trade.getPaymentRequest().getFundingTxHash();
        Transaction fundingTx = getEscrowTransaction(fundingTxHash);
        String signature = getPayoutSignature(trade, fundingTx);

        Coin payoutAmount = Coin.parseCoin(trade.getBuyRequest().getBtcAmount().toPlainString());

        ECKey arbitratorProfilePubKey = ECKey.fromPublicOnly(Base58.decode(trade.getSellOffer().getArbitratorProfilePubKey()));
        ECKey sellerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getSellOffer().getSellerEscrowPubKey()));
        ECKey buyerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(trade.getBuyRequest().getBuyerEscrowPubKey()));

        Address buyerPayoutAddress = Address.fromBase58(netParams, trade.getBuyRequest().getBuyerPayoutAddress());

        TransactionSignature sellerSignature = TransactionSignature
                .decodeFromBitcoin(Base58.decode(signature), true, true);

        TransactionSignature buyerSignature = TransactionSignature
                .decodeFromBitcoin(Base58.decode(trade.getPayoutRequest().getPayoutTxSignature()), true, true);

        Transaction payoutTx = new Transaction(netParams);
        payoutTx.setPurpose(Transaction.Purpose.ASSURANCE_CONTRACT_CLAIM);

        // order signatures in lexicographical order or pubkey bytes
        List<TransactionSignature> signatures;
        if (ECKey.PUBKEY_COMPARATOR.compare(sellerEscrowPubKey, buyerEscrowPubKey) < 0) {
            signatures = Arrays.asList(sellerSignature, buyerSignature);
        } else {
            signatures = Arrays.asList(buyerSignature, sellerSignature);
        }

        Address escrowAddress = escrowAddress(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey);
        Script redeemScript = redeemScript(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey);

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
        payoutTx.addOutput(payoutAmount, buyerPayoutAddress);

        LOG.debug("Validate inputs for payoutTx: " + payoutTx.toString());
        for (TransactionInput input : payoutTx.getInputs()) {
            LOG.debug("Validating input for payoutTx: " + input.toString());
            try {
                input.verify(input.getConnectedOutput());
                LOG.debug("Input valid for payoutTx: " + input.toString());
            } catch (VerificationException ve) {
                LOG.error("Input not valid for payoutTx, " + ve.getMessage());
            } catch (NullPointerException npe) {
                LOG.error("Null connectedOutput for payoutTx");
            }
        }

        escrowWallet.commitTx(payoutTx);
        TransactionBroadcast bc = peerGroup.broadcastTransaction(payoutTx);
        return payoutTx.getHash().toString();
    }

    public String sendCoins(SendRequest sendRequest) throws InsufficientMoneyException {
        Transaction tx = tradeWallet.sendCoins(sendRequest).tx;
        return tx.getHashAsString();
    }

    public void addWatchedEscrowAddress(String escrowAddress) {
        Context.propagate(btcContext);
        Address address = Address.fromBase58(getNetParams(), escrowAddress);
        if (escrowWallet.addWatchedAddress(address, (DateTime.now().getMillis() / 1000) - 600)) {
            LOG.debug("Added watched address: {}", address.toBase58());
        } else {
            LOG.warn("Failed to add watch address: {}", address.toBase58());
        }
    }

    public void removeWatchedEscrowAddress(String escrowAddress) {
        Address address = Address.fromBase58(getNetParams(), escrowAddress);
        Context.propagate(btcContext);
        if (escrowWallet.removeWatchedAddress(address)) {
            LOG.debug("Removed watched address: {}", address.toBase58());
        } else {
            LOG.warn("Failed to remove watched address: {}", address.toBase58());
        }
    }
}
