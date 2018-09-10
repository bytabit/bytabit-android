package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.trade.model.TradeManagerException;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.fasterxml.jackson.jr.ob.JSON;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bytabit.mobile.trade.model.Trade.Role.*;
import static com.bytabit.mobile.trade.model.Trade.Status.*;

public class TradeManager {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    WalletManager walletManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    SellerProtocol sellerProtocol;

    @Inject
    BuyerProtocol buyerProtocol;

    @Inject
    ArbitratorProtocol arbitratorProtocol;

    private static final String CURRENT_TRADE_JSON = "currentTrade.json";

    private static final String TRADES_PATH = AppConfig.getPrivateStorage().getPath() + File.separator +
            "trades" + File.separator;

    private final TradeService tradeService;

    private final PublishSubject<Trade> createdTradeSubject;

    private final ConnectableObservable<Trade> createdTrade;

    private final PublishSubject<Trade> updatedTradeSubject;

    private final Observable<Trade> updatedTrade;

    private final PublishSubject<Trade> selectedTradeSubject;

    private final Observable<Trade> selectedTrade;

    private final ConnectableObservable<Trade> lastSelectedTrade;

    public TradeManager() {
        tradeService = new TradeService();

        createdTradeSubject = PublishSubject.create();

        createdTrade = createdTradeSubject
                .doOnSubscribe(d -> log.debug("createdTrade: subscribe"))
                .doOnNext(ct -> log.debug("createdTrade: {}", ct.getEscrowAddress()))
                .replay();

        updatedTradeSubject = PublishSubject.create();

        updatedTrade = updatedTradeSubject
                .doOnSubscribe(d -> log.debug("updatedTrade: subscribe"))
                //.doOnNext(ut -> log.debug("updatedTrade: {}", ut.getEscrowAddress()))
                .share();

        selectedTradeSubject = PublishSubject.create();

        selectedTrade = selectedTradeSubject
                .doOnSubscribe(d -> log.debug("selectedTrade: subscribe"))
                .doOnNext(st -> log.debug("selectedTrade: {}", st.getEscrowAddress()))
                .share();

        lastSelectedTrade = selectedTrade
                .doOnSubscribe(d -> log.debug("lastSelectedTrade: subscribe"))
                .doOnNext(ct -> log.debug("lastSelectedTrade: {}", ct.getEscrowAddress()))
                .replay(1);
    }

    @PostConstruct
    public void initialize() {

        createTradesDir();

        createdTrade.connect();
        lastSelectedTrade.connect();

        Maybe<Boolean> walletsSynced = Observable.zip(walletManager.getTradeDownloadProgress().autoConnect(),
                walletManager.getEscrowDownloadProgress().autoConnect(), (tp, ep) -> tp == 1 && ep == 1)
                .filter(p -> p)
                .map(p -> true)
                .firstElement()
                .observeOn(Schedulers.io())
                .doOnSubscribe(d -> log.debug("walletSynced: subscribe"))
                .doOnSuccess(p -> log.debug("walletSynced: {}", p))
                .cache();

        // get stored trades after download progress is 100% loaded
        walletsSynced.subscribe(p -> getStoredTrades()
                .flatMapIterable(t -> t)
                .flatMapMaybe(t -> walletManager.getEscrowTransactionWithAmt(t.getFundingTxHash())
                        .map(txa -> {
                            t.fundingTransactionWithAmt(txa);
                            return t;
                        }).defaultIfEmpty(t))
                .flatMapMaybe(t -> walletManager.getEscrowTransactionWithAmt(t.getPayoutTxHash())
                        .map(txa -> {
                            t.payoutTransactionWithAmt(txa);
                            return t;
                        }).defaultIfEmpty(t))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                //.doOnNext(walletManager::createOrLoadEscrowWallet)
                .subscribe(createdTradeSubject::onNext));

        // get updated trades after download progress is 100% loaded
        walletsSynced.subscribe(p -> profileManager.loadOrCreateMyProfile().toObservable()
                .flatMap(profile -> Observable.interval(15, TimeUnit.SECONDS, Schedulers.io())
                        .flatMap(t -> tradeService.get(profile.getPubKey())
                                .retryWhen(error -> error.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS)))
                                .flattenAsObservable(l -> l))
                        .flatMapMaybe(trade -> handleReceivedTrade(profile, trade)))
                // store updated trade
                .flatMapSingle(this::writeTrade)
                .doOnError(t -> log.error("writeTrade: error {}", t.getMessage()))
                // put updated trade
                .flatMapSingle(tradeService::put)
                .doOnError(t -> log.error("putTrade: error {}", t.getMessage()))
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext));
    }

    public Single<Trade> createTrade(SellOffer sellOffer, BigDecimal btcAmount) {

        return buyerProtocol.createTrade(sellOffer, btcAmount)
                .flatMap(this::writeTrade)
                .flatMap(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .doOnSuccess(createdTradeSubject::onNext);
    }

    public Observable<Trade> getCreatedTrade() {
        return createdTrade;
    }

    public Observable<Trade> getUpdatedTrade() {
        return updatedTrade;
    }

    public void setSelectedTrade(Trade trade) {
        selectedTradeSubject.onNext(trade);
    }

    public Observable<Trade> getSelectedTrade() {
        return selectedTrade
                .doOnNext(trade -> log.debug("Selected: {}", trade.getEscrowAddress()));
    }

    public ConnectableObservable<Trade> getLastSelectedTrade() {
        return lastSelectedTrade;
    }

    private Observable<List<Trade>> getStoredTrades() {
        return Single.<List<Trade>>create(source -> {
            // load stored trades
            List<Trade> trades = new ArrayList<>();
            File tradesDir = new File(TRADES_PATH);
            if (!tradesDir.exists()) {
                tradesDir.mkdirs();
            }
            if (tradesDir.list() != null) {
                for (String tradeId : tradesDir.list()) {
                    try {
                        File tradeFile = new File(TRADES_PATH + tradeId + File.separator + CURRENT_TRADE_JSON);
                        if (tradeFile.exists()) {
                            FileReader tradeReader = new FileReader(tradeFile);
                            Trade currentTrade = JSON.std.beanFrom(Trade.class, tradeReader);
                            trades.add(currentTrade);
                        }
                    } catch (IOException ioe) {
                        source.onError(ioe);
                    }
                }
            } else {
                tradesDir.mkdirs();
            }
            source.onSuccess(trades);
        }).toObservable();
    }

    public void buyerSendPayment(String paymentReference) {
        getLastSelectedTrade().autoConnect()
                .filter(trade -> trade.status().equals(FUNDED))
                .flatMapMaybe(st -> buyerProtocol.sendPayment(st, paymentReference))
                .flatMapSingle(this::writeTrade)
                .flatMapSingle(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .doOnNext(updatedTradeSubject::onNext)
                .subscribe();
    }

    public void sellerConfirmPaymentReceived() {
        getLastSelectedTrade().autoConnect()
                .filter(trade -> PAID.equals(trade.status()))
                .firstElement().flatMap(st -> sellerProtocol.confirmPaymentReceived(st))
                .flatMapSingle(this::writeTrade)
                .flatMap(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext);
    }

    public void requestArbitrate() {

        getLastSelectedTrade().autoConnect()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) != 0)
                .filter(trade -> trade.status().compareTo(CREATED) > 0)
                .filter(trade -> trade.status().compareTo(ARBITRATING) < 0)
                .firstElement().flatMap(trade -> getProtocol(trade).requestArbitrate(trade))
                .flatMapSingle(this::writeTrade)
                .flatMap(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext);
    }

    public void arbitratorRefundSeller() {
        getLastSelectedTrade().autoConnect()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) == 0)
                .filter(trade -> trade.status().compareTo(ARBITRATING) == 0)
                .firstElement().flatMap(trade -> arbitratorProtocol.refundSeller(trade))
                .flatMapSingle(this::writeTrade)
                .flatMap(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext);
    }

    public void arbitratorPayoutBuyer() {
        getLastSelectedTrade().autoConnect()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) == 0)
                .filter(trade -> trade.status().compareTo(ARBITRATING) == 0)
                .firstElement().flatMap(trade -> arbitratorProtocol.payoutBuyer(trade))
                .flatMapSingle(this::writeTrade)
                .flatMap(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext);
    }

    public void cancelAndRefundSeller() {
        getLastSelectedTrade().autoConnect()
                .filter(trade -> trade.getRole().compareTo(BUYER) == 0)
                .filter(trade -> trade.status().compareTo(FUNDED) == 0)
                .firstElement().flatMap(trade -> buyerProtocol.refundTrade(trade))
                .flatMapSingle(this::writeTrade)
                .flatMap(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext);
    }

    private Maybe<Trade> handleReceivedTrade(Profile profile, Trade receivedTrade) {

        Single<Trade> currentTrade = readTrade(receivedTrade.getEscrowAddress())
                .toSingle(createdFromReceivedTrade(receivedTrade));

        Single<Trade> currentTradeWithRole = currentTrade
                .map(ct -> setRole(ct, profile.getPubKey()));

        Single<Trade> currentTradeWithTx = currentTradeWithRole
                .flatMap(this::updateTradeTx);

        return currentTradeWithTx.flatMapMaybe(ct -> updateTrade(ct, receivedTrade));
    }

    private Trade createdFromReceivedTrade(Trade receivedTrade) {

        // TODO validate escrowAddress, sellOffer, buyRequest
        String escrowAddress = walletManager.escrowAddress(receivedTrade.getArbitratorProfilePubKey(),
                receivedTrade.getSellerEscrowPubKey(),
                receivedTrade.getBuyerEscrowPubKey());

        return Trade.builder()
                .escrowAddress(escrowAddress)
                .createdTimestamp(LocalDateTime.now())
                .sellOffer(receivedTrade.sellOffer())
                .buyRequest(receivedTrade.buyRequest())
                .build();
    }

    private void createTradesDir() {

        File tradesDir = new File(TRADES_PATH);
        if (!tradesDir.exists()) {
            tradesDir.mkdirs();
        }
    }

    private Single<Trade> updateTradeTx(Trade trade) {

        return walletManager.getEscrowTransactionWithAmt(trade.getFundingTxHash())
                .map(trade::fundingTransactionWithAmt)
                .toSingle(trade)
                .flatMap(t -> walletManager.getEscrowTransactionWithAmt(t.getPayoutTxHash())
                        .map(t::payoutTransactionWithAmt)
                        .toSingle(t));
    }

    private Maybe<Trade> updateTrade(Trade trade, Trade receivedTrade) {

        TradeProtocol tradeProtocol = getProtocol(trade);

        Maybe<Trade> tradeUpdated = Maybe.empty();

        switch (trade.status()) {

            case CREATED:
                tradeUpdated = tradeProtocol.handleCreated(trade, receivedTrade);
                break;

            case FUNDING:
                tradeUpdated = tradeProtocol.handleFunded(trade, receivedTrade);
                break;

            case FUNDED:
                tradeUpdated = tradeProtocol.handleFunded(trade, receivedTrade);
                break;

            case CANCELING:
                tradeUpdated = Maybe.just(trade);
                break;

            case CANCELED:
                tradeUpdated = Maybe.just(trade);
                break;

            case PAID:
                tradeUpdated = tradeProtocol.handlePaid(trade, receivedTrade);
                break;

            case ARBITRATING:
                tradeUpdated = tradeProtocol.handleArbitrating(trade, receivedTrade);
                break;

            case COMPLETING:
                tradeUpdated = Maybe.empty();
                break;

            case COMPLETED:
                tradeUpdated = Maybe.just(trade);
                break;
        }

        return tradeUpdated;
    }

    private Trade setRole(Trade trade, String profilePubKey) {

        if (trade.getSellerProfilePubKey().equals(profilePubKey)) {
            trade.setRole(SELLER);
        } else if (trade.getBuyerProfilePubKey().equals(profilePubKey)) {
            trade.setRole(BUYER);
        } else if (trade.getArbitratorProfilePubKey().equals(profilePubKey)) {
            trade.setRole(ARBITRATOR);
        } else {
            throw new TradeManagerException("Unable to determine trade role.");
        }
        return trade;
    }

    TradeProtocol getProtocol(Trade trade) {

        Trade.Role role = trade.getRole();

        if (role.equals(SELLER)) {
            return sellerProtocol;
        } else if (role.equals(BUYER)) {
            return buyerProtocol;
        } else if (role.equals(ARBITRATOR)) {
            return arbitratorProtocol;
        } else {
            throw new TradeManagerException("Unable to determine trade protocol.");
        }
    }

    protected Single<Trade> writeTrade(Trade trade) {

        return Single.create(source -> {
            String tradePath = TRADES_PATH + trade.getEscrowAddress() + File.separator + CURRENT_TRADE_JSON;

            try {
                File dir = new File(TRADES_PATH + trade.getEscrowAddress());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileWriter tradeWriter = new FileWriter(tradePath);
                tradeWriter.write(JSON.std.asString(trade));
                tradeWriter.flush();

            } catch (IOException ioe) {
                source.onError(ioe);
            }
            source.onSuccess(trade);
        });
    }

    protected Maybe<Trade> readTrade(String escrowAddress) {

        return Maybe.create(source -> {
            try {
                File tradeFile = new File(TRADES_PATH + escrowAddress + File.separator + CURRENT_TRADE_JSON);
                if (tradeFile.exists()) {
                    FileReader tradeReader = new FileReader(tradeFile);
                    Trade trade = JSON.std.beanFrom(Trade.class, tradeReader);
                    source.onSuccess(trade);
                } else {
                    source.onComplete();
                }
            } catch (Exception ex) {
                source.onError(ex);
            }
        });
    }
}