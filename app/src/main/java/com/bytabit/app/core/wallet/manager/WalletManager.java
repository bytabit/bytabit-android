/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.app.core.wallet.manager;

import com.bytabit.app.core.common.AppConfig;
import com.bytabit.app.core.wallet.model.TradeWalletInfo;
import com.bytabit.app.core.wallet.model.TransactionWithAmt;
import com.bytabit.app.core.wallet.model.WalletKitConfig;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Service;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import static java.lang.Boolean.TRUE;
import static org.bitcoinj.wallet.DeterministicKeyChain.BIP44_ACCOUNT_ZERO_PATH;

@Slf4j
@Singleton
public class WalletManager {

    private final AppConfig appConfig;
    private final Executor executor;

    private final long ONE_WEEK_MILLISECONDS = 60000 * 60 * 24 * 7;

    private final NetworkParameters netParams;
    private final Context btcContext;

    private BehaviorSubject<WalletKitConfig> tradeWalletConfig = BehaviorSubject.create();
    private Observable<BytabitWalletAppKit> tradeWalletAppKit;
    private Observable<TransactionWithAmt> tradeUpdatedWalletTx;

    private BehaviorSubject<WalletKitConfig> escrowWalletConfig = BehaviorSubject.create();
    private Observable<BytabitWalletAppKit> escrowWalletAppKit;

    private Observable<Double> walletsDownloadProgress;
    private Observable<Boolean> walletsRunning;
    private Observable<Boolean> walletSynced;
    private Observable<String> profilePubKey;

    @Inject
    public WalletManager(AppConfig appConfig, @Named("wallet") Executor executor) {
        this.appConfig = appConfig;
        this.executor = executor;

        netParams = BytabitTestNet3Params.fromID("org.bitcoin." + appConfig.getBtcNetwork());
        btcContext = Context.getOrCreate(netParams);

        initialize();
    }

    //@PostConstruct
    public void initialize() {

        // setup trade wallet progress monitoring

        BehaviorSubject<Double> tradeDownloadProgressSubject = BehaviorSubject.create();

        Observable<Double> tradeDownloadProgress = tradeDownloadProgressSubject
                .startWith(0.0)
                .doOnSubscribe(d -> log.debug("tradeDownloadProgress: subscribe"))
                .doOnNext(progress -> log.debug("tradeDownloadProgress: {}%", BigDecimal.valueOf(progress).setScale(0, BigDecimal.ROUND_DOWN)))
                .replay(1).autoConnect();

        WalletKitConfig tradeConfig = WalletKitConfig.builder().netParams(netParams)
                .directory(appConfig.getAppStorage()).filePrefix("trade").build();

        // setup escrow wallet progress monitoring

        BehaviorSubject<Double> escrowDownloadProgressSubject = BehaviorSubject.create();

        Observable<Double> escrowDownloadProgress = escrowDownloadProgressSubject
                .startWith(0.0)
                .doOnSubscribe(d -> log.debug("escrowDownloadProgress: subscribe"))
                .doOnNext(progress -> log.debug("escrowDownloadProgress: {}%", BigDecimal.valueOf(progress).setScale(0, BigDecimal.ROUND_DOWN)))
                .replay(1).autoConnect();

        // monitor download progress

        walletsDownloadProgress = Observable.combineLatest(tradeDownloadProgress, escrowDownloadProgress, (tp, ep) -> {
            if (tp > ep) {
                return ep;
            } else {
                return tp;
            }
        })
                .throttleLast(1, TimeUnit.SECONDS)
                .doOnSubscribe(d -> log.debug("walletsDownloadProgress: subscribe"))
                .doOnNext(p -> log.debug("walletsDownloadProgress: {}", p))
                .replay(100).autoConnect();

        // start trade wallet

        tradeWalletAppKit = tradeWalletConfig.scan(createWalletAppKit(tradeConfig, null), this::reloadWallet)
                .doOnNext(tw -> setDownloadListener(tw, tradeDownloadProgressSubject))
                .doOnNext(this::start)
                .replay(1).autoConnect();

        tradeUpdatedWalletTx = tradeWalletAppKit
                .map(WalletAppKit::wallet)
                .switchMap(tw -> Observable.<TransactionWithAmt>create(source -> {

                    TransactionConfidenceEventListener listener = (wallet, tx) -> source.onNext(createTransactionWithAmt(wallet, tx));
                    tw.addTransactionConfidenceEventListener(executor, listener);

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

        // start escrow wallet

        WalletKitConfig escrowConfig = WalletKitConfig.builder().netParams(netParams)
                .directory(appConfig.getAppStorage()).filePrefix("escrow").build();

        escrowWalletAppKit = escrowWalletConfig.scan(createWalletAppKit(escrowConfig, null), this::reloadWallet)
                .doOnNext(ew -> setDownloadListener(ew, escrowDownloadProgressSubject))
                .doOnNext(this::start)
                .replay(1).autoConnect();

        // triggers for wallet start and synced

        Observable<Boolean> tradeWalletsRunning = tradeWalletAppKit
                .switchMap(tw -> Observable.<Boolean>create(source -> {
                    Service.Listener listener = new Service.Listener() {
                        @Override
                        public void running() {
                            source.onNext(TRUE);
                        }

                        @Override
                        public void stopping(Service.State from) {
                            source.onNext(Boolean.FALSE);
                        }
                    };
                    tw.addListener(listener, executor);
                    source.onNext(tw.isRunning());
                }))
                .observeOn(Schedulers.io())
                .doOnSubscribe(d -> log.debug("tradeWalletRunning: subscribe"))
                .doOnNext(p -> log.debug("tradeWalletRunning: {}", p));

        Observable<Boolean> escrowWalletsRunning = escrowWalletAppKit
                .switchMap(ew -> Observable.<Boolean>create(source -> {
                    Service.Listener listener = new Service.Listener() {
                        @Override
                        public void running() {
                            source.onNext(TRUE);
                        }

                        @Override
                        public void stopping(Service.State from) {
                            source.onNext(Boolean.FALSE);
                        }
                    };
                    ew.addListener(listener, executor);
                    source.onNext(ew.isRunning());
                }))
                .observeOn(Schedulers.io())
                .doOnSubscribe(d -> log.debug("escrowWalletRunning: subscribe"))
                .doOnNext(p -> log.debug("EscrowWalletRunning: {}", p));

        walletsRunning = Observable.combineLatest(tradeWalletsRunning, escrowWalletsRunning, (tr, er) -> tr && er)
                .startWith(false)
                .replay(1).autoConnect();

        walletSynced = walletsRunning.filter(r -> r.equals(true))
                .flatMap(r -> walletsDownloadProgress)
                .map(p -> p == 100.00)
                .startWith(false)
                .distinctUntilChanged()
                .doOnSubscribe(d -> log.debug("walletSynced: subscribe"))
                .doOnNext(p -> log.debug("walletSynced: {}", p))
                .observeOn(Schedulers.io())
                .replay(1).autoConnect();

        profilePubKey = tradeWalletAppKit.map(BytabitWalletAppKit::wallet)
                .map(this::getBase58ProfilePubKey)
                .doOnSubscribe(d -> log.debug("profilePubKey: subscribe"))
                .doOnNext(p -> log.debug("profilePubKey: {}", p))
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
        BytabitWalletAppKit walletAppKit = new BytabitWalletAppKit(appConfig, walletKitConfig, currentSeed);

        log.debug("created walletAppKit with config {}", walletKitConfig);

        return walletAppKit;
    }

    private void setDownloadListener(BytabitWalletAppKit wak, BehaviorSubject<Double> subject) {

        wak.setDownloadListener(new DownloadProgressTracker() {

            @Override
            protected void progress(double pct, int blocksSoFar, Date date) {
                super.progress(pct, blocksSoFar, date);
                subject.onNext(pct);
            }

            @Override
            protected void doneDownload() {
                super.doneDownload();
                subject.onNext(100.00);
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

    public Observable<Double> getWalletsDownloadProgress() {
        return walletsDownloadProgress;
    }

    public Maybe<TradeWalletInfo> getTradeWalletInfo() {
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet)
                .map(this::getTradeWalletInfo);
    }

    public Maybe<BigDecimal> getTradeWalletBalance() {
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet)
                .map(Wallet::getBalance)
                .map(c -> new BigDecimal(c.toPlainString()));
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

    public Maybe<String> getProfilePubKeyBase58() {
        return profilePubKey.firstElement();
//                .doOnSubscribe(d -> log.debug("profilePubKeyBase58: subscribe"))
//                .doOnSuccess(p -> log.debug("profilePubKeyBase58: {}", p));
    }

    public Observable<String> getProfilePubKey() {
        return profilePubKey;
    }

    public Maybe<String> getEscrowPubKeyBase58() {
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet).map(this::getFreshBase58ReceivePubKey);
        //.doOnSubscribe(d -> log.debug("escrowPubKeyBase58: subscribe"))
        //.doOnSuccess(p -> log.debug("escrowPubKeyBase58: {}", p));
    }

    public Maybe<String> getTradePubKeyBase58() {
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet).map(this::getFreshBase58ReceivePubKey);
        //.doOnSubscribe(d -> log.debug("escrowPubKeyBase58: subscribe"))
        //.doOnSuccess(p -> log.debug("escrowPubKeyBase58: {}", p));
    }

    public Maybe<String> getDepositAddressBase58() {
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet)
                .map(this::getDepositAddress)
                .map(Address::toBase58);
    }

    public Maybe<Address> getDepositAddress() {
        return tradeWalletAppKit.firstElement()
                .map(WalletAppKit::wallet).map(this::getDepositAddress);
    }

    public Maybe<TransactionWithAmt> withdrawFromTradeWallet(String withdrawAddress, BigDecimal withdrawAmount, BigDecimal txFeePerKb) {

        return tradeWalletAppKit.firstElement().map(WalletAppKit::wallet).flatMap(w ->
                {
                    try {
                        Context.propagate(btcContext);
                        return broadcastTransaction(w, Coin.parseCoin(withdrawAmount.toPlainString()),
                                Coin.parseCoin(txFeePerKb.toString()), Address.fromBase58(netParams, withdrawAddress))
                                .map(tx -> createTransactionWithAmt(w, tx));
                    } catch (AddressFormatException afe) {
                        throw new WalletException("Invalid withdraw address format.");
                    } catch (IllegalArgumentException iae) {
                        throw new WalletException("Invalid bitcoin amount.");
                    }
                }
        );
    }

    public void watchNewEscrowAddressAndResetBlockchain(String escrowAddress) {
        getWatchedEscrowAddresses()
                .doOnSuccess(eal -> eal.add(Address.fromBase58(netParams, escrowAddress)))
                .subscribe(eal -> escrowWalletConfig.onNext(WalletKitConfig.builder()
                        .netParams(netParams)
                        .directory(appConfig.getAppStorage())
                        .filePrefix("escrow")
                        .creationDate(null)//new Date(System.currentTimeMillis() - ONE_WEEK_MILLISECONDS * 2))
                        .watchAddresses(eal)
                        .build()));
    }

    public Maybe<String> watchNewEscrowAddress(String escrowAddress) {
        return getEscrowWallet()
                .map(ew -> ew.addWatchedAddress(Address.fromBase58(netParams, escrowAddress), System.currentTimeMillis() - ONE_WEEK_MILLISECONDS * 2))
                .filter(s -> s.equals(true))
                .map(s -> escrowAddress);
    }

    private Single<List<Address>> getWatchedEscrowAddresses() {
        return getEscrowWallet().map(ew -> ew.getWatchedScripts())
                .flattenAsObservable(scripts -> scripts)
                .filter(script -> script.isPayToScriptHash())
                .map(script -> script.getToAddress(netParams))
                .toList();
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

    private String getBase58ProfilePubKey(Wallet tradeWallet) {
        Context.propagate(btcContext);
        DeterministicKey profileKey = getProfilePubKey(tradeWallet);
        return Base58.encode(profileKey.getPubKey());
    }

    private DeterministicKey getProfilePubKey(Wallet tradeWallet) {
        Context.propagate(btcContext);
        return tradeWallet.getActiveKeyChain().getKeyByPath(BIP44_ACCOUNT_ZERO_PATH, true);
    }

    private TradeWalletInfo getTradeWalletInfo(Wallet tradeWallet) {
        Context.propagate(btcContext);
        return TradeWalletInfo.builder()
                .profilePubKey(getBase58ProfilePubKey(tradeWallet))
                .seedWords(getSeedWords(tradeWallet))
                .xpubKey(getXpubKey(tradeWallet))
                .xprvKey(getXprvKey(tradeWallet))
                .build();
    }

    private String getFreshBase58ReceivePubKey(Wallet wallet) {
        Context.propagate(btcContext);
        return Base58.encode(wallet.freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).getPubKey());
    }

    public BigDecimal defaultTxFee() {
        return new BigDecimal(defaultTxFeeCoin().toPlainString());
    }

    public Coin defaultTxFeeCoin() {
        return Coin.valueOf(106000);
    }

    public Maybe<Transaction> fundEscrow(String escrowAddress, BigDecimal amount, BigDecimal txFeePerKb) {
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
        BigDecimal amountPlusPayoutTxFee = amount.add(txFeePerKb);

        return notFundedWallet.flatMap(tw ->
                broadcastTransaction(tw, Coin.parseCoin(amountPlusPayoutTxFee.toString()), Coin.parseCoin(txFeePerKb.toString()), Address.fromBase58(netParams, escrowAddress))
        );
    }

    private Maybe<Transaction> broadcastTransaction(Wallet wallet, Coin amount, Coin txFeePerKb, Address address) {
        return Maybe.create(source -> {
            try {
                Context.propagate(btcContext);
                SendRequest sendRequest = SendRequest.to(address, amount);
                sendRequest.feePerKb = txFeePerKb;
                Wallet.SendResult sendResult = wallet.sendCoins(sendRequest);
                source.onSuccess(sendResult.tx);
            } catch (InsufficientMoneyException ex) {
                log.error("Insufficient wallet balance to create transaction for {} BTC.", amount.toPlainString());
                // TODO let user know not enough BTC in wallet
                source.onError(new WalletException(String.format("Insufficient wallet balance to create transaction for %s BTC.", amount.toPlainString())));
            } catch (Wallet.DustySendRequested dsr) {
                source.onError(new WalletException("Withdraw amount must be greater than dust limit (" + Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.multiply(3).toFriendlyString() + ")."));
            } catch (Exception ex) {
                log.error("Error while broadcasting transaction.", ex);
                source.onError(ex);
            }
        });
    }

    private Address escrowAddress(ECKey arbitratorProfilePubKey,
                                  ECKey sellerEscrowPubKey,
                                  ECKey buyerEscrowPubKey) {
        return ScriptBuilder.createP2SHOutputScript(redeemScript(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey)).getToAddress(netParams);
    }

    public String escrowAddress(String arbitratorProfilePubKeyBase58,
                                String sellerEscrowPubKeyBase58,
                                String buyerEscrowPubKeyBase58) {

        ECKey apk = ECKey.fromPublicOnly(Base58.decode(arbitratorProfilePubKeyBase58));
        ECKey spk = ECKey.fromPublicOnly(Base58.decode(sellerEscrowPubKeyBase58));
        ECKey bpk = ECKey.fromPublicOnly(Base58.decode(buyerEscrowPubKeyBase58));

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

    public Maybe<String> getPayoutSignature(BigDecimal btcAmount,
                                            Transaction fundingTransaction,
                                            String arbitratorProfilePubKeyBase58,
                                            String sellerEscrowPubKeyBase58,
                                            String buyerEscrowPubKeyBase58,
                                            String payoutAddressBase58) {

        Coin payoutAmount = Coin.parseCoin(btcAmount.toPlainString());
        ECKey arbitratorProfilePubKey = ECKey.fromPublicOnly(Base58.decode(arbitratorProfilePubKeyBase58));
        ECKey sellerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(sellerEscrowPubKeyBase58));
        ECKey buyerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(buyerEscrowPubKeyBase58));
        Address payoutAddress = Address.fromBase58(netParams, payoutAddressBase58);

        return getPayoutSignature(payoutAmount,
                fundingTransaction,
                arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey,
                payoutAddress)
                .map(TransactionSignature::encodeToBitcoin)
                .map(Base58::encode);
    }

    public Maybe<ECKey> getProfileECKey() {

        return getTradeWallet().map(this::getProfilePubKey);
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
                        && txo.getValue().compareTo(outputAmount) == 0) {

                    // post payout input and funding output with empty unlock scripts
                    TransactionInput input = payoutTx.addInput(txo);
                    Script emptyUnlockScript = ScriptBuilder.createP2SHMultiSigInputScript(null, redeemScript);
                    input.setScriptSig(emptyUnlockScript);
                    break;
                }
            }

            if (payoutTx.getInputs().size() != 1) {
                return Maybe.error(new WalletException(String.format("Did not find escrow funding TX %s or matching output to address %s with amount %s", fundingTx.getHashAsString(), escrowAddress, payoutAmount.toFriendlyString())));
            }

            // add output to payout tx
            payoutTx.addOutput(payoutAmount, payoutAddress);

            // find signing key
            ECKey escrowKey = tw.findKeyFromPubKey(buyerEscrowPubKey.getPubKey());
            if (escrowKey == null) {
                escrowKey = tw.findKeyFromPubKey(sellerEscrowPubKey.getPubKey());
            }
            if (escrowKey == null) {
                escrowKey = getProfilePubKey(tw);
            }
            if (escrowKey != null) {
                // sign tx input
                Sha256Hash unlockSigHash = payoutTx.hashForSignature(0, redeemScript, Transaction.SigHash.ALL, false);
                return Maybe.just(new TransactionSignature(escrowKey.sign(unlockSigHash), Transaction.SigHash.ALL, false));
            } else {
                return Maybe.error(new WalletException("Can not create payout signature, no signing key found."));
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

    public Maybe<String> payoutEscrowToBuyer(BigDecimal btcAmount, BigDecimal txFeePerKb,
                                             Transaction fundingTransaction,
                                             String arbitratorProfilePubKeyBase58,
                                             String sellerEscrowPubKeyBase58,
                                             String buyerEscrowPubKeyBase58,
                                             String payoutAddressBase58,
                                             String payoutTxSignatureBase58) {

        Coin payoutAmount = Coin.parseCoin(btcAmount.toPlainString());
        Coin txFeePerKbAmount = Coin.parseCoin(txFeePerKb.toPlainString());
        ECKey arbitratorProfilePubKey = ECKey.fromPublicOnly(Base58.decode(arbitratorProfilePubKeyBase58));
        ECKey sellerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(sellerEscrowPubKeyBase58));
        ECKey buyerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(buyerEscrowPubKeyBase58));
        Address payoutAddress = Address.fromBase58(netParams, payoutAddressBase58);

        Maybe<TransactionSignature> mySignature = getPayoutSignature(payoutAmount, fundingTransaction,
                arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey,
                payoutAddress);

        Maybe<TransactionSignature> buyerSignature = Maybe.just(TransactionSignature
                .decodeFromBitcoin(Base58.decode(payoutTxSignatureBase58), true, true));

        Single<List<TransactionSignature>> signatures = mySignature.concatWith(buyerSignature).toList();

        return signatures.flatMapMaybe(sl -> payoutEscrow(payoutAmount, txFeePerKbAmount, fundingTransaction,
                arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey,
                payoutAddress, sl).map(Sha256Hash::toString));
    }

    public Maybe<String> refundEscrowToSeller(BigDecimal btcAmount, BigDecimal txFeePerKb,
                                              Transaction fundingTransaction,
                                              String arbitratorProfilePubKeyBase58,
                                              String sellerEscrowPubKeyBase58,
                                              String buyerEscrowPubKeyBase58,
                                              String refundAddressBase58,
                                              String refundTxSignatureBase58,
                                              boolean isArbitrator) {

        Coin payoutAmount = Coin.parseCoin(btcAmount.toPlainString());
        Coin txFeePerKbAmount = Coin.parseCoin(txFeePerKb.toPlainString());
        ECKey arbitratorProfilePubKey = ECKey.fromPublicOnly(Base58.decode(arbitratorProfilePubKeyBase58));
        ECKey sellerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(sellerEscrowPubKeyBase58));
        ECKey buyerEscrowPubKey = ECKey.fromPublicOnly(Base58.decode(buyerEscrowPubKeyBase58));
        Address refundAddress = Address.fromBase58(netParams, refundAddressBase58);

        Maybe<TransactionSignature> mySignature = getPayoutSignature(payoutAmount, fundingTransaction,
                arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey,
                refundAddress);

        Maybe<TransactionSignature> sellerRefundSignature = Maybe.just(TransactionSignature
                .decodeFromBitcoin(Base58.decode(refundTxSignatureBase58), true, true));

        Single<List<TransactionSignature>> signatures;
        if (isArbitrator) {
            signatures = mySignature.concatWith(sellerRefundSignature).toList();
        } else {
            signatures = sellerRefundSignature.concatWith(mySignature).toList();
        }

        return signatures.flatMapMaybe(sl -> payoutEscrow(payoutAmount, txFeePerKbAmount, fundingTransaction,
                arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey,
                refundAddress, sl).map(Sha256Hash::toString));
    }

    public Observable<Boolean> getWalletsRunning() {
        return walletsRunning;
    }

    public Observable<Boolean> getWalletSynced() {
        return walletSynced;
    }

    public WalletKitConfig restoreTradeWallet(@NonNull List<String> mnemonicCode, @NonNull Date creationDate) {

        // validate words
        if (mnemonicCode.size() < 12) {
            throw new WalletException("Wallet restore requires 12 mnemonic code words.");
        }

        // validate date
        if (creationDate.compareTo(new Date()) >= 0) {
            throw new WalletException("Wallet restore creation date must be before current time.");
        }

        WalletKitConfig walletKitConfig = WalletKitConfig.builder()
                .netParams(netParams)
                .directory(appConfig.getAppStorage())
                .filePrefix("trade")
                .mnemonicCode(mnemonicCode)
                .creationDate(creationDate)
                .build();

        tradeWalletConfig.onNext(walletKitConfig);

        return walletKitConfig;
    }

    public Observable<WalletKitConfig> getTradeWalletConfig() {
        return tradeWalletConfig;
    }

    public Single<String> getBase58PubKeySignature(Sha256Hash hash) {
        return getPubKeySignature(hash)
                .map(ECKey.ECDSASignature::encodeToDER)
                .map(Base58::encode)
                .toSingle()
                .onErrorResumeNext(t -> {
                    if (t instanceof NoSuchElementException) {
                        return Single.error(new WalletException("Unable to sign hash with pubkey"));
                    } else {
                        return Single.error(t);
                    }
                });
    }

    public boolean validateBase58PubKeySignature(String pubKey, String signature, Sha256Hash hash) {
        ECKey pubECKey = ECKey.fromPublicOnly(Base58.decode(pubKey));
        ECKey.ECDSASignature ecdsaSignature = ECKey.ECDSASignature.decodeFromDER(Base58.decode(signature));

        return pubECKey.verify(hash, ecdsaSignature);
    }

    private Maybe<Sha256Hash> payoutEscrow(Coin payoutAmount, Coin txFeePerKb,
                                           Transaction fundingTx,
                                           ECKey arbitratorProfilePubKey,
                                           ECKey sellerEscrowPubKey,
                                           ECKey buyerEscrowPubKey,
                                           Address payoutAddress,
                                           List<TransactionSignature> signatures) {

        Transaction payoutTx = new Transaction(netParams);
        payoutTx.setPurpose(Transaction.Purpose.ASSURANCE_CONTRACT_CLAIM);

        Script redeemScript = redeemScript(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey);
        Address escrowAddress = escrowAddress(arbitratorProfilePubKey, sellerEscrowPubKey, buyerEscrowPubKey);

        // add input to payout tx from single matching funding tx output
        for (TransactionOutput txo : fundingTx.getOutputs()) {
            Address outputAddress = txo.getAddressFromP2SH(netParams);
            Coin expectedOutputAmount = payoutAmount.plus(txFeePerKb);

            // verify output from fundingTx exists and equals required payout amounts
            if (outputAddress != null && outputAddress.equals(escrowAddress)
                    && txo.getValue().equals(expectedOutputAmount)) {

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
                if (input.getConnectedOutput() == null) {
                    log.error("Null connectedOutput for payoutTx");
                    return Maybe.error(new WalletException("Null connectedOutput for payoutTx"));
                }
                input.verify(input.getConnectedOutput());
                log.debug("Input valid for payoutTx: {}", input);
            } catch (VerificationException ve) {
                log.error("Input not valid for payoutTx, {}", ve.getMessage());
                return Maybe.error(new WalletException(String.format("Input not valid for payoutTx, {}", ve.getMessage())));
            }
        }

        return Maybe.zip(getEscrowWallet(), getEscrowPeerGroup(), (ew, pg) -> {
            Context.propagate(btcContext);
            ew.commitTx(payoutTx);
            pg.broadcastTransaction(payoutTx);
            return payoutTx.getHash();
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

    private Maybe<ECKey.ECDSASignature> getPubKeySignature(Sha256Hash hash) {

        return getTradeWallet()
                .map(this::getProfilePubKey)
                .map(pk -> pk.sign(hash));
    }
}
