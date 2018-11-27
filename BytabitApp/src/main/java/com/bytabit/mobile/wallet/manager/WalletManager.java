package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.bytabit.mobile.wallet.model.WalletKitConfig;
import com.bytabit.mobile.wallet.model.WalletManagerException;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WalletManager {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final NetworkParameters netParams;
    private final Context btcContext;

    private BehaviorSubject<WalletKitConfig> tradeWalletConfig = BehaviorSubject.create();
    private Observable<BytabitWalletAppKit> tradeWalletAppKit;
    private Observable<Double> tradeDownloadProgress;
    private Observable<TransactionWithAmt> tradeUpdatedWalletTx;

    private BehaviorSubject<WalletKitConfig> escrowWalletConfig = BehaviorSubject.create();
    private Observable<BytabitWalletAppKit> escrowWalletAppKit;
    private Observable<Double> escrowDownloadProgress;

    private Observable<Boolean> walletRunning;
    private Observable<Boolean> walletSynced;

    public WalletManager() {
        netParams = BytabitTestNet3Params.fromID("org.bitcoin." + AppConfig.getBtcNetwork());
        btcContext = Context.getOrCreate(netParams);
    }

    @PostConstruct
    public void initialize() {

        // setup trade wallet

        BehaviorSubject<Double> tradeDownloadProgressSubject = BehaviorSubject.create();
        tradeDownloadProgress = tradeDownloadProgressSubject
                .doOnSubscribe(d -> log.debug("tradeDownloadProgress: subscribe"))
                .doOnNext(progress -> log.debug("tradeDownloadProgress: {}%", BigDecimal.valueOf(progress * 100.00).setScale(0, BigDecimal.ROUND_DOWN)))
                .throttleLast(1, TimeUnit.SECONDS)
                .observeOn(Schedulers.io())
                .replay(100).autoConnect();

        WalletKitConfig tradeConfig = WalletKitConfig.builder().netParams(netParams)
                .directory(AppConfig.getPrivateStorage()).filePrefix("trade").build();

        tradeWalletAppKit = tradeWalletConfig.scan(createWalletAppKit(tradeConfig, null), this::reloadWallet)
                .doOnNext(tw -> setDownloadListener(tw, tradeDownloadProgressSubject))
                .doOnNext(this::start)
                .replay(1).autoConnect();

        tradeUpdatedWalletTx = tradeWalletAppKit
                .map(WalletAppKit::wallet)
                .switchMap(tw -> Observable.<TransactionWithAmt>create(source -> {

                    TransactionConfidenceEventListener listener = (wallet, tx) -> source.onNext(createTransactionWithAmt(wallet, tx));
                    tw.addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);

                    source.setCancellable(() -> {
                        log.debug("tradeUpdatedWalletTx: removeTransactionConfidenceEventListener");
                        tw.removeTransactionConfidenceEventListener(listener);
                    });
                }).startWith(Observable.fromIterable(tw.getTransactions(false))
                        .map(tx -> createTransactionWithAmt(tw, tx))))
                .groupBy(TransactionWithAmt::getHash)
                .flatMap(txg -> txg.throttleLast(1, TimeUnit.SECONDS))
                .doOnSubscribe(d -> log.debug("tradeUpdatedWalletTx: subscribe"))
                .doOnNext(tx -> log.debug("tradeUpdatedWalletTx: {}", tx.getHash()))
                .observeOn(Schedulers.io())
                .replay(20, TimeUnit.MINUTES).autoConnect();

        // setup escrow wallet

        BehaviorSubject<Double> escrowDownloadProgressSubject = BehaviorSubject.create();
        escrowDownloadProgress = escrowDownloadProgressSubject
                .doOnSubscribe(d -> log.debug("escrowDownloadProgress: subscribe"))
                .doOnNext(progress -> log.debug("escrowDownloadProgress: {}%", BigDecimal.valueOf(progress * 100.00).setScale(0, BigDecimal.ROUND_DOWN)))
                .throttleLast(1, TimeUnit.SECONDS)
                .observeOn(Schedulers.io())
                .replay(100).autoConnect();


        WalletKitConfig escrowConfig = WalletKitConfig.builder().netParams(netParams)
                .directory(AppConfig.getPrivateStorage()).filePrefix("escrow").build();

        escrowWalletAppKit = escrowWalletConfig.scan(createWalletAppKit(escrowConfig, null), this::reloadWallet)
                .doOnNext(tw -> setDownloadListener(tw, escrowDownloadProgressSubject))
                .doOnNext(this::start)
                .replay(1).autoConnect();

//        escrowWalletAppKit = Single.just(WalletKitConfig.builder().netParams(netParams)
//                .directory(AppConfig.getPrivateStorage()).filePrefix("escrow").build())
//                .map(this::createWalletAppKit)
//                .doOnSuccess(wak -> setDownloadListener(wak, escrowDownloadProgressSubject))
//                .doOnSuccess(this::start)
//                .cache();

        // triggers for wallet start and synced

        walletRunning = tradeWalletAppKit
                .switchMap(tw -> Observable.<Boolean>create(source -> {
                    Listener listener = new Listener() {
                        @Override
                        public void running() {
                            source.onNext(Boolean.TRUE);
                        }

                        @Override
                        public void stopping(State from) {
                            source.onNext(Boolean.FALSE);
                        }
                    };
                    tw.addListener(listener, BytabitMobile.EXECUTOR);
                    source.onNext(tw.isRunning());
                }))
                .observeOn(Schedulers.io())
                .doOnSubscribe(d -> log.debug("walletRunning: subscribe"))
                .doOnNext(p -> log.debug("walletRunning: {}", p))
                .replay(1).autoConnect();

        walletSynced = Observable.zip(tradeDownloadProgress, escrowDownloadProgress,
                (tp, ep) -> tp == 1 && ep == 1)
                .startWith(Boolean.FALSE)
                .observeOn(Schedulers.io())
                .doOnSubscribe(d -> log.debug("walletSynced: subscribe"))
                .doOnNext(p -> log.debug("walletSynced: {}", p))
                .replay(1).autoConnect();

        // TODO shutdown wallet?
    }

    private BytabitWalletAppKit reloadWallet(BytabitWalletAppKit currentWallet, WalletKitConfig config) {

        DeterministicSeed currentSeed = null;
        if (currentWallet.isRunning()) {
            currentSeed = currentWallet.wallet().getKeyChainSeed();
            stop(currentWallet);
        }
        return createWalletAppKit(config, currentSeed);
    }

    private BytabitWalletAppKit createWalletAppKit(WalletKitConfig walletKitConfig, DeterministicSeed currentSeed) {
        BytabitWalletAppKit walletAppKit = new BytabitWalletAppKit(walletKitConfig, currentSeed);

        log.debug("created walletAppKit with config {}", walletKitConfig);

        return walletAppKit;
    }

    private void setDownloadListener(BytabitWalletAppKit wak, BehaviorSubject<Double> subject) {

        wak.setDownloadListener(new DownloadProgressTracker() {

            @Override
            protected void progress(double pct, int blocksSoFar, Date date) {
                super.progress(pct, blocksSoFar, date);
                subject.onNext(pct / 100.00);
            }

            @Override
            protected void doneDownload() {
                super.doneDownload();
                subject.onNext(1.00);
            }
        });
        log.debug("added download listener for {}", wak.getFilePrefix());
    }

    private void start(BytabitWalletAppKit wak) {
        log.debug("starting walletAppKit: {}", wak.getFilePrefix());
        wak.startAsync();
        wak.awaitRunning();
        log.debug("started walletAppKit: {}", wak.getFilePrefix());
    }

    private void stop(BytabitWalletAppKit wak) {
        log.debug("stopping {}", wak.getFilePrefix());
        wak.stopAsync();
        wak.awaitTerminated();
        log.debug("stopped {}", wak.getFilePrefix());
    }

    public Observable<Double> getTradeDownloadProgress() {
        return tradeDownloadProgress;
    }

    public Observable<Double> getEscrowDownloadProgress() {
        return escrowDownloadProgress;
    }

    public Maybe<TradeWalletInfo> getTradeWalletInfo() {
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet)
                .map(this::getTradeWalletInfo);
    }

    public Observable<TransactionWithAmt> getTradeUpdatedWalletTx() {
        return tradeUpdatedWalletTx;
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
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet).map(this::getFreshBase58AuthPubKey);
    }

    public Maybe<String> getTradeWalletEscrowPubKey() {
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet).map(this::getFreshBase58ReceivePubKey);
    }

    public Maybe<Address> getTradeWalletDepositAddress() {
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet).map(this::getDepositAddress);
    }

    public Maybe<TransactionWithAmt> withdrawFromTradeWallet(String withdrawAddress, BigDecimal withdrawAmount) {
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet).map(w -> {
                    Address address = Address.fromBase58(netParams, withdrawAddress);
                    Coin amount = Coin.parseCoin(withdrawAmount.toPlainString());
                    SendRequest request = SendRequest.to(address, amount);
                    Wallet.SendResult sendResult = w.sendCoins(request);
                    return createTransactionWithAmt(w, sendResult.tx);
                });
    }

    public Maybe<String> watchEscrowAddressAndResetBlockchain(String escrowAddress) {

//        watchEscrowAddress(escrowAddress).map(ea -> WalletKitConfig.builder()
//                .netParams(netParams)
//                .directory(AppConfig.getPrivateStorage()).filePrefix("escrow")
//                .creationDate(creationDate)
//                .build();
//
//        tradeWalletConfig.onNext(walletKitConfig);

        return watchEscrowAddress(escrowAddress);
    }

    public Maybe<String> watchEscrowAddress(String escrowAddress) {

        return escrowWalletAppKit.firstElement()
                .map(WalletAppKit::wallet)
                .map(ew -> ew.addWatchedAddress(Address.fromBase58(netParams, escrowAddress), (ZonedDateTime.now().toEpochSecond())))
                .filter(s -> s.equals(true))
                .map(s -> escrowAddress);
    }

    private Maybe<Wallet> getTradeWallet() {
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet);
    }

    private Maybe<Wallet> getEscrowWallet() {

        return escrowWalletAppKit.firstElement()
                .map(WalletAppKit::wallet);
    }

    private Maybe<PeerGroup> getEscrowPeerGroup() {

        return escrowWalletAppKit.firstElement()
                .map(WalletAppKit::peerGroup);
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
        return Coin.valueOf(106000);
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
                sendRequest.feePerKb = defaultTxFeeCoin();
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

        return getTradeWallet().flatMap(tw -> {
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
                return Single.just(new TransactionSignature(escrowKey.sign(unlockSigHash), Transaction.SigHash.ALL, false)).toMaybe();
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

    public Observable<Boolean> getWalletRunning() {
        return walletRunning;
    }

    public Observable<Boolean> getWalletSynced() {
        return walletSynced;
    }

    public void restoreTradeWallet(List<String> mnemonicCode, LocalDate creationDate) {

        WalletKitConfig walletKitConfig = WalletKitConfig.builder()
                .netParams(netParams)
                .directory(AppConfig.getPrivateStorage()).filePrefix("trade")
                .mnemonicCode(mnemonicCode)
                .creationDate(creationDate)
                .build();

        tradeWalletConfig.onNext(walletKitConfig);
    }

    public Observable<WalletKitConfig> getTradeWalletConfig() {
        return tradeWalletConfig;
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

        return Maybe.zip(getEscrowWallet(), getEscrowPeerGroup(), (ew, pg) -> {
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
