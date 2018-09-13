package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.model.ManagedWallet;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.bytabit.mobile.wallet.model.WalletManagerException;
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

import javax.annotation.PostConstruct;
import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bytabit.mobile.wallet.manager.WalletManager.Command.*;
import static org.bitcoinj.core.Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;

public class WalletManager {

    public enum Command {
        START,
        STOP,
        RESET
    }

    private static final String WALLET_FILE_EXT = ".wallet";
    private static final String SPVCHAIN_FILE_EXT = ".spvchain";

    private static final String BACKUP_EXT = ".bkp";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final NetworkParameters netParams;
    private final Context btcContext;

    private final Subject<Command> tradeCommandSubject;
    private final Subject<Command> escrowCommandSubject;

    private ConnectableObservable<ManagedWallet> tradeManagedWallet;
    private ConnectableObservable<ManagedWallet> escrowManagedWallet;

    private ConnectableObservable<Double> tradeDownloadProgress;
    private ConnectableObservable<Double> escrowDownloadProgress;

    private ConnectableObservable<TransactionWithAmt> tradeUpdatedWalletTx;
    private ConnectableObservable<TransactionWithAmt> escrowUpdatedWalletTx;

    public WalletManager() {
        netParams = NetworkParameters.fromID("org.bitcoin." + AppConfig.getBtcNetwork());
        btcContext = Context.getOrCreate(netParams);
        tradeCommandSubject = PublishSubject.create();
        escrowCommandSubject = PublishSubject.create();
    }

    @PostConstruct
    public void initialize() {

        tradeManagedWallet = tradeCommandSubject.startWith(START)
                .doOnNext(c -> log.debug("trade command: {}", c))
                .switchMap(c -> {
                    Observable<ManagedWallet> managedWallet;

                    if (START.equals(c)) {
                        managedWallet = createManagedWallet("trade", false);
                    } else if (RESET.equals(c)) {
                        managedWallet = createManagedWallet("trade", true);
                    } else {
                        managedWallet = Observable.empty();
                    }

                    return managedWallet.doOnTerminate(() -> log.debug("tradeManagedWallet: terminate"))
                            .doOnSubscribe(d -> log.debug("tradeManagedWallet: subscribe"))
                            .doOnDispose(() -> log.debug("tradeManagedWallet: dispose"))
                            .doOnComplete(() -> log.debug("tradeManagedWallet: complete"))
                            .doOnError(t -> log.error("tradeManagedWallet: error {}", t.getMessage()))
                            .retry(1);

                })
                .doOnNext(mw -> log.debug("tradeManagedWallet: {}", mw))
                .replay(1);

        escrowManagedWallet = escrowCommandSubject.startWith(START)
                .doOnNext(c -> log.debug("escrow command: {}", c))
                .switchMap(c -> {
                    Observable<ManagedWallet> managedWallet;

                    if (START.equals(c)) {
                        managedWallet = createManagedWallet("escrow", false);
                    } else if (RESET.equals(c)) {
                        managedWallet = createManagedWallet("escrow", true);
                    } else {
                        managedWallet = Observable.empty();
                    }

                    return managedWallet.doOnTerminate(() -> log.debug("escrowManagedWallet: terminate"))
                            .doOnSubscribe(d -> log.debug("escrowManagedWallet: subscribe"))
                            .doOnDispose(() -> log.debug("escrowManagedWallet: dispose"))
                            .doOnComplete(() -> log.debug("escrowManagedWallet: complete"))
                            .doOnError(t -> log.error("escrowManagedWallet: error {}", t.getMessage()))
                            .retry(1);

                })
                .doOnNext(mw -> log.debug("tradeManagedWallet: {}", mw))
                .replay(1);

        tradeUpdatedWalletTx = tradeManagedWallet.autoConnect()
                .map(ManagedWallet::getWallet)
                .switchMap(tw -> Observable.<TransactionWithAmt>create(source -> {

                    Observable.fromIterable(tw.getTransactions(false))
                            .subscribe(tx -> source.onNext(createTransactionWithAmt(tw, tx)));

                    TransactionConfidenceEventListener listener = (wallet, tx) -> source.onNext(createTransactionWithAmt(wallet, tx));

                    tw.addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);

                    source.setCancellable(() -> {
                        log.debug("tradeUpdatedWalletTx: removeTransactionConfidenceEventListener");
                        tw.removeTransactionConfidenceEventListener(listener);
                    });
                }).groupBy(TransactionWithAmt::getHash).flatMap(txg ->
                        txg.throttleLast(1, TimeUnit.SECONDS)))
                .doOnSubscribe(d -> log.debug("tradeUpdatedWalletTx: subscribe"))
                .doOnNext(tx -> log.debug("tradeUpdatedWalletTx: {}", tx.getHash()))
                .replay(20, TimeUnit.MINUTES);

        // post observable download events
        tradeDownloadProgress = tradeManagedWallet.autoConnect()
                .map(ManagedWallet::getPeerGroup)
                .switchMap(this::downloadListener).subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> log.debug("tradeDownloadProgress: subscribe"))
                .doOnNext(progress -> log.debug("tradeDownloadProgress: {}%", BigDecimal.valueOf(progress * 100.00).setScale(0, BigDecimal.ROUND_DOWN)))
                .throttleLast(1, TimeUnit.SECONDS)
                .replay(100);

        escrowDownloadProgress = escrowManagedWallet.autoConnect()
                .map(ManagedWallet::getPeerGroup)
                .switchMap(this::downloadListener).subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> log.debug("escrowDownloadProgress: subscribe"))
                .doOnNext(progress -> log.debug("escrowDownloadProgress: {}%", BigDecimal.valueOf(progress * 100.00).setScale(0, BigDecimal.ROUND_DOWN)))
                .throttleLast(1, TimeUnit.SECONDS)
                .replay(100);

        BytabitMobile.getNavEvents().distinctUntilChanged()
                .filter(ne -> ne.equals(BytabitMobile.NavEvent.QUIT))
                .subscribe(qe -> {
                    tradeCommandSubject.onNext(STOP);
                    escrowCommandSubject.onNext(STOP);
                });
    }

    private Observable<ManagedWallet> createManagedWallet(String name, boolean deleteBlockstore) {

        // block store file

        Single<File> blockStoreFile = Single.just(new File(AppConfig.getPrivateStorage(), name + SPVCHAIN_FILE_EXT));

        // create wallet

        Observable<Wallet> wallet = Observable.<Wallet>create(source -> {
            try {
                source.onNext(createOrLoadTradeWallet(name));
            } catch (IOException | UnreadableWalletException e) {
                source.onError(e);
            }
        }).doOnError(t -> log.debug("{} wallet: error {}", name, t.getMessage())).retry(1).cache();

        // create block store

        Observable<BlockStore> blockStore = blockStoreFile.toObservable().map(bsf -> {
            Context.propagate(btcContext);
            if (deleteBlockstore && bsf.exists()) {
                Files.delete(bsf.toPath());
            }
            BlockStore bs = new SPVBlockStore(netParams, bsf);
            bs.getChainHead(); // detect corruptions as early as possible
            return bs;
        }).doOnError(t -> log.debug("{} blockStore: error {}", name, t.getMessage())).retry(1);

        // create block chain

        Observable<BlockChain> blockChain = Observable.zip(wallet, blockStore, (w, bs) -> {
            Context.propagate(btcContext);
            return new BlockChain(netParams, w, bs);
        });

        // create peer group

        Observable<PeerGroup> peerGroup = Observable.zip(wallet, blockChain, this::createPeerGroup)
                .doOnNext(PeerGroup::start);

        return Observable.zip(wallet, peerGroup, (w, pg) -> new ManagedWallet(name, w, pg));
    }

    private Observable<Double> downloadListener(PeerGroup pg) {

        return Observable.create(source -> pg.startBlockChainDownload(new DownloadProgressTracker() {

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
        }));
    }

    public ConnectableObservable<Double> getTradeDownloadProgress() {
        return tradeDownloadProgress;
    }

    public ConnectableObservable<Double> getEscrowDownloadProgress() {
        return escrowDownloadProgress;
    }

    public Observable<TradeWalletInfo> getTradeWalletInfo() {
        return tradeManagedWallet.autoConnect().map(ManagedWallet::getWallet)
                .map(this::getTradeWalletInfo);
    }

    public ConnectableObservable<TransactionWithAmt> getTradeUpdatedWalletTx() {
        return tradeUpdatedWalletTx;
    }

    public ConnectableObservable<TransactionWithAmt> getUpdatedEscrowWalletTx() {
        return escrowUpdatedWalletTx;
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
        return tradeManagedWallet.autoConnect().firstElement()
                .map(ManagedWallet::getWallet).map(this::getFreshBase58AuthPubKey);
    }

    public Maybe<String> getTradeWalletEscrowPubKey() {
        return tradeManagedWallet.autoConnect().firstElement()
                .map(ManagedWallet::getWallet).map(this::getFreshBase58ReceivePubKey);
    }

    public Maybe<Address> getTradeWalletDepositAddress() {
        return tradeManagedWallet.autoConnect().firstElement()
                .map(ManagedWallet::getWallet).map(this::getDepositAddress);
    }

    private PeerGroup createPeerGroup(Wallet wallet, BlockChain blockChain) throws UnknownHostException {

        Context.propagate(btcContext);

        PeerGroup peerGroup = new PeerGroup(netParams, blockChain);
        peerGroup.setUserAgent("org.bytabit.mobile", AppConfig.getVersion());

        peerGroup.addWallet(wallet);

        if (netParams.equals(RegTestParams.get())) {
            peerGroup.setMaxConnections(1);
            peerGroup.addAddress(new PeerAddress(netParams, InetAddress.getLocalHost(), netParams.getPort()));

        } else {
            peerGroup.addPeerDiscovery(new DnsDiscovery(netParams));
        }

        peerGroup.setFastCatchupTimeSecs(wallet.getEarliestKeyCreationTime());

        return peerGroup;
    }

    private Wallet createOrLoadTradeWallet(String name)
            throws IOException, UnreadableWalletException {

        final File walletFile = new File(AppConfig.getPrivateStorage(), name + WALLET_FILE_EXT);
        final File walletBackupFile = new File(AppConfig.getPrivateStorage(), name + WALLET_FILE_EXT + BACKUP_EXT);
        return createOrLoadWallet(walletFile, walletBackupFile);
    }

    public Maybe<String> watchEscrowAddressAndResetBlockchain(String escrowAddress) {

        return watchEscrowAddress(escrowAddress)
                .doOnSuccess(ea -> escrowCommandSubject.onNext(RESET));
    }

    public Maybe<String> watchEscrowAddress(String escrowAddress) {

        return escrowManagedWallet.autoConnect().firstElement()
                .map(ManagedWallet::getWallet)
                .map(ew -> ew.addWatchedAddress(Address.fromBase58(netParams, escrowAddress), (DateTime.now().getMillis() / 1000)))
                .filter(s -> s.equals(true))
                .map(s -> escrowAddress);
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

                wallet = loadWallet(walletBackupFile);
                wallet.reset();
                log.debug("restoreWallet ({}): {}", e1.getMessage(), walletBackupFile.getPath());
                saveWallet(wallet, walletFile);
                throw e1;
            }
        } else {
            log.debug("createWallet: {}", walletFile.getPath());
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
        try (FileInputStream walletInputStream = new FileInputStream(walletFile)) {
            final WalletProtobufSerializer serializer = new WalletProtobufSerializer();
            wallet = serializer.readWallet(walletInputStream);

            if (!wallet.getParams().equals(netParams)) {
                log.error("Invalid network params in wallet file: {}", walletFile);
                throw new UnreadableWalletException("Wrong network parameters, found: " + wallet.getParams().getId() + " should be: " + netParams.getId());
            }
            log.debug("loadWallet: {}", walletFile.getPath());

        } catch (final IOException e) {
            log.error("Unable to close wallet input stream {}: {}", walletFile, e.getCause());
            throw e;
        }

        return wallet;
    }

    private void saveWallet(Wallet wallet, File walletFile) throws IOException {

        final Protos.Wallet walletProto = new WalletProtobufSerializer().walletToProto(wallet);

        try (OutputStream walletOutputStream = new FileOutputStream(walletFile)) {

            walletProto.writeTo(walletOutputStream);
            walletOutputStream.flush();
            log.debug("savedWallet: {}", walletFile.getPath());
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

        try (OutputStream walletOutputStream = new FileOutputStream(walletBackupFile)) {
            walletProto.writeTo(walletOutputStream);
            walletOutputStream.flush();
        } catch (final IOException e) {
            log.error("Can't save wallet backup", e);
        }
    }

    private Maybe<Wallet> getTradeWallet() {
        return tradeManagedWallet.autoConnect().firstElement()
                .map(ManagedWallet::getWallet);
    }

    private Maybe<Wallet> getEscrowWallet() {

        return escrowManagedWallet.autoConnect().firstElement()
                .map(ManagedWallet::getWallet);
    }

    private Maybe<PeerGroup> getPeerGroup() {

        return tradeManagedWallet.autoConnect().firstElement()
                .map(ManagedWallet::getPeerGroup);
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

        Address address = Address.fromBase58(netParams, escrowAddress);

        // verify no outputs to escrow address already created
        Maybe<Wallet> notFundedWallet = getTradeWallet().flattenAsObservable(tw -> tw.getTransactions(false))
                .flatMapIterable(Transaction::getOutputs)
                .any(txo -> {
                    Address outputAddress = txo.getAddressFromP2SH(netParams);
                    return outputAddress != null && outputAddress.equals(address);
                })
                .filter(f -> f.equals(false))
                .flatMap(f -> getTradeWallet());

        // TODO determine correct amount for extra tx fee for payout, current using DEFAULT_TX_FEE
        return notFundedWallet.flatMap(tw -> Maybe.create(source -> {
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

    private String getWatchedOutputAddress(Transaction tx, Wallet wallet) {

        Address address;
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

        return watchedOutputAddresses.isEmpty() ? null : watchedOutputAddresses.get(0);
    }

    public Maybe<TransactionWithAmt> getEscrowTransactionWithAmt(String txHash) {

        return getEscrowWallet().flatMap(w -> {
            Transaction tx = txHash != null ? w.getTransaction(Sha256Hash.wrap(txHash)) : null;
            Maybe<Transaction> maybeTx = tx != null ? Maybe.just(tx) : Maybe.empty();
            return maybeTx.map(t -> createTransactionWithAmt(w, t));
        });
    }

    public Maybe<String> getPayoutSignature(Trade fundedTrade) {
        Address buyerPayoutAddress = Address.fromBase58(netParams, fundedTrade.getBuyerPayoutAddress());
        return getPayoutSignature(fundedTrade, fundedTrade.getFundingTransactionWithAmt().getTransaction(), buyerPayoutAddress);
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
                throw new WalletManagerException("Can not create payout signature, no signing key found.");
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

        Maybe<String> signature = getPayoutSignature(trade, trade.getFundingTransactionWithAmt().getTransaction(), buyerPayoutAddress);

        Maybe<TransactionSignature> mySignature = signature.map(s -> TransactionSignature
                .decodeFromBitcoin(Base58.decode(s), true, true));

        Maybe<TransactionSignature> buyerSignature = Maybe.just(TransactionSignature
                .decodeFromBitcoin(Base58.decode(trade.getPayoutTxSignature()), true, true));

        Single<List<TransactionSignature>> signatures = mySignature.concatWith(buyerSignature).toList();

        return signatures.flatMapMaybe(sl -> payoutEscrow(trade, buyerPayoutAddress, sl));
    }

    public Maybe<String> refundEscrowToSeller(Trade trade) {

        Address sellerRefundAddress = Address.fromBase58(netParams, trade.getRefundAddress());

        Maybe<String> signature = getPayoutSignature(trade, trade.getFundingTransactionWithAmt().getTransaction(), sellerRefundAddress);

        Maybe<TransactionSignature> mySignature = signature.map(s -> TransactionSignature
                .decodeFromBitcoin(Base58.decode(s), true, true));

        Maybe<TransactionSignature> sellerSignature = Maybe.just(TransactionSignature
                .decodeFromBitcoin(Base58.decode(trade.getRefundTxSignature()), true, true));

        Single<List<TransactionSignature>> signatures;
        if (trade.getRole().equals(Trade.Role.ARBITRATOR)) {
            signatures = mySignature.concatWith(sellerSignature).toList();
        } else if (trade.getRole().equals(Trade.Role.BUYER)) {
            signatures = sellerSignature.concatWith(mySignature).toList();
        } else {
            throw new WalletManagerException("Only arbitrator or buyer roles can refund escrow to seller.");
        }

        return signatures.flatMapMaybe(sl -> payoutEscrow(trade, sellerRefundAddress, sl));
    }

    private Maybe<String> payoutEscrow(Trade trade, Address payoutAddress,
                                       List<TransactionSignature> signatures) {

        Transaction fundingTx = trade.getFundingTransactionWithAmt().getTransaction();

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

        log.debug("Validate inputs for payoutTx: {}", payoutTx);
        for (TransactionInput input : payoutTx.getInputs()) {
            log.debug("Validating input for payoutTx: {}", input);
            try {
                input.verify(input.getConnectedOutput());
                log.debug("Input valid for payoutTx: {}", input);
            } catch (VerificationException ve) {
                log.error("Input not valid for payoutTx, {}", ve.getMessage());
            } catch (NullPointerException npe) {
                log.error("Null connectedOutput for payoutTx");
            }
        }

        return Maybe.zip(getEscrowWallet(), getPeerGroup(), (ew, pg) -> {
            Context.propagate(btcContext);
            ew.commitTx(payoutTx);
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
