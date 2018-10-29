package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.bytabit.mobile.wallet.model.WalletKitConfig;
import com.bytabit.mobile.wallet.model.WalletManagerException;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
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
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
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

    private Single<BytabitWalletAppKit> escrowWalletAppKit;
    private Observable<Double> escrowDownloadProgress;
    private Observable<TransactionWithAmt> escrowUpdatedWalletTx;

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

//        WalletKitConfig tradeConfig2 = WalletKitConfig.builder().netParams(netParams)
//                .directory(AppConfig.getPrivateStorage()).filePrefix("trade")
//                .deterministicSeed(new DeterministicSeed(Arrays.asList("canyon", "shoulder", "absent", "angle", "problem", "soon", "baby", "foster", "evidence", "ready", "cost", "raw"), null, "", 0))
//                .build();

        tradeWalletAppKit = tradeWalletConfig.startWith(tradeConfig)
                .scan(Maybe.<BytabitWalletAppKit>empty(), (wak, newConfig) ->
                        wak.filter(BytabitWalletAppKit::isRunning)
                                .doOnSuccess(this::stop)
                                .map(ak -> createWalletAppKit(newConfig))
                                .defaultIfEmpty(createWalletAppKit(newConfig)))
                .flatMapMaybe(wak -> wak)
                .doOnNext(wak -> setDownloadListener(wak, tradeDownloadProgressSubject))
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

        escrowWalletAppKit = Single.just(WalletKitConfig.builder().netParams(netParams)
                .directory(AppConfig.getPrivateStorage()).filePrefix("escrow").build())
                .map(this::createWalletAppKit)
                .doOnSuccess(wak -> setDownloadListener(wak, escrowDownloadProgressSubject))
                .doOnSuccess(this::start)
                .cache();

        // TODO shutdown wallet?
    }

    private BytabitWalletAppKit createWalletAppKit(WalletKitConfig walletConfig) {
        BytabitWalletAppKit walletAppKit = new BytabitWalletAppKit(walletConfig);

        if (walletConfig.getNetParams().equals(RegTestParams.get())) {
            walletAppKit.connectToLocalHost();
        }

        if (walletConfig.getDeterministicSeed() != null) {
            walletAppKit.restoreWalletFromSeed(walletConfig.getDeterministicSeed());
        }

        walletAppKit.setBlockingStartup(false);
        walletAppKit.setAutoSave(true);
        walletAppKit.setUserAgent("org.bytabit.mobile", AppConfig.getVersion());
        log.debug("created walletAppKit with config {}", walletConfig);

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

    public Observable<TransactionWithAmt> getUpdatedEscrowWalletTx() {
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

        return watchEscrowAddress(escrowAddress);
    }

    public Maybe<String> watchEscrowAddress(String escrowAddress) {

        return escrowWalletAppKit
                .map(WalletAppKit::wallet)
                .map(ew -> ew.addWatchedAddress(Address.fromBase58(netParams, escrowAddress), (DateTime.now().getMillis() / 1000)))
                .filter(s -> s.equals(true))
                .map(s -> escrowAddress);
    }

    private Maybe<Wallet> getTradeWallet() {
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet);
    }

    private Single<Wallet> getEscrowWallet() {

        return escrowWalletAppKit
                .map(WalletAppKit::wallet);
    }

    private Single<PeerGroup> getEscrowPeerGroup() {

        return escrowWalletAppKit
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

        return getEscrowWallet().flatMapMaybe(w -> {
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

        return Single.zip(getEscrowWallet(), getEscrowPeerGroup(), (ew, pg) -> {
            Context.propagate(btcContext);
            ew.commitTx(payoutTx);
            pg.broadcastTransaction(payoutTx);
            return payoutTx.getHash().toString();
        }).toMaybe();
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
