package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.trade.model.TradeManagerException;
import com.bytabit.mobile.wallet.manager.WalletManager;
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
import java.math.BigDecimal;
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

    private final TradeService tradeService;

    private final TradeStorage tradeStorage;

    private final PublishSubject<Trade> createdTradeSubject;

    private final ConnectableObservable<Trade> createdTrade;

    private final PublishSubject<Trade> updatedTradeSubject;

    private final Observable<Trade> updatedTrade;

    private final PublishSubject<Trade> selectedTradeSubject;

    private final Observable<Trade> selectedTrade;

    private final ConnectableObservable<Trade> lastSelectedTrade;

    public TradeManager() {

        tradeService = new TradeService();

        tradeStorage = new TradeStorage();

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

        tradeStorage.createTradesDir();

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
        walletsSynced.subscribe(p -> tradeStorage.getStoredTrades()
                .flatMapIterable(t -> t)
                .flatMapMaybe(t -> walletManager.getEscrowTransactionWithAmt(t.getFundingTxHash())
                        .map(txa -> t.copyBuilder().fundingTransactionWithAmt(txa).build())
                        .defaultIfEmpty(t))
                .flatMapMaybe(t -> walletManager.getEscrowTransactionWithAmt(t.getPayoutTxHash())
                        .map(txa -> t.copyBuilder().payoutTransactionWithAmt(txa).build())
                        .defaultIfEmpty(t))
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
                .flatMapSingle(tradeStorage::writeTrade)
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
                .flatMap(tradeStorage::writeTrade)
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

    public ConnectableObservable<Trade> getLastSelectedTrade() {
        return lastSelectedTrade;
    }

    public void buyerSendPayment(String paymentReference) {
        getLastSelectedTrade().autoConnect()
                .filter(trade -> trade.getStatus().equals(FUNDED))
                .flatMapMaybe(st -> buyerProtocol.sendPayment(st, paymentReference))
                .flatMapSingle(tradeStorage::writeTrade)
                .flatMapSingle(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .doOnNext(updatedTradeSubject::onNext)
                .subscribe();
    }

    public void sellerConfirmPaymentReceived() {
        getLastSelectedTrade().autoConnect()
                .filter(trade -> PAID.equals(trade.getStatus()))
                .firstElement().flatMap(st -> sellerProtocol.confirmPaymentReceived(st))
                .flatMapSingle(tradeStorage::writeTrade)
                .flatMap(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext);
    }

    public void requestArbitrate() {

        getLastSelectedTrade().autoConnect()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) != 0)
                .filter(trade -> trade.getStatus().compareTo(CREATED) > 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) < 0)
                .firstElement().flatMap(trade -> getProtocol(trade).requestArbitrate(trade))
                .flatMapSingle(tradeStorage::writeTrade)
                .flatMap(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext);
    }

    public void arbitratorRefundSeller() {
        getLastSelectedTrade().autoConnect()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) == 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) == 0)
                .firstElement().flatMap(trade -> arbitratorProtocol.refundSeller(trade))
                .flatMapSingle(tradeStorage::writeTrade)
                .flatMap(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext);
    }

    public void arbitratorPayoutBuyer() {
        getLastSelectedTrade().autoConnect()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) == 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) == 0)
                .firstElement().flatMap(trade -> arbitratorProtocol.payoutBuyer(trade))
                .flatMapSingle(tradeStorage::writeTrade)
                .flatMap(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext);
    }

    public void cancelAndRefundSeller() {
        getLastSelectedTrade().autoConnect()
                .filter(trade -> trade.getRole().compareTo(BUYER) == 0)
                .filter(trade -> trade.getStatus().compareTo(FUNDED) == 0)
                .firstElement().flatMap(trade -> buyerProtocol.refundTrade(trade))
                .flatMapSingle(tradeStorage::writeTrade)
                .flatMap(tradeService::put)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext);
    }

    private Maybe<Trade> handleReceivedTrade(Profile profile, Trade receivedTrade) {

        Single<Trade> currentTrade = tradeStorage.readTrade(receivedTrade.getEscrowAddress())
                .toSingle(createdFromReceivedTrade(receivedTrade));

        Single<Trade> currentTradeWithRole = currentTrade
                .map(ct -> ct.withRole(profile.getPubKey()));

        Single<Trade> currentTradeWithStatus = currentTradeWithRole
                .map(Trade::withStatus);

        Single<Trade> currentTradeWithTx = currentTradeWithStatus
                .flatMap(this::updateTradeTx);

        return currentTradeWithTx.flatMapMaybe(ct -> updateTrade(ct, receivedTrade.withRole(profile.getPubKey()).withStatus()));
    }

    private Trade createdFromReceivedTrade(Trade receivedTrade) {

        // TODO validate escrowAddress, sellOffer, buyRequest
        String escrowAddress = walletManager.escrowAddress(receivedTrade.getArbitratorProfilePubKey(),
                receivedTrade.getSellerEscrowPubKey(),
                receivedTrade.getBuyerEscrowPubKey());

        return Trade.builder()
                .escrowAddress(escrowAddress)
                .createdTimestamp(LocalDateTime.now())
                .sellOffer(receivedTrade.getSellOffer())
                .buyRequest(receivedTrade.getBuyRequest())
                .build();
    }

    private Single<Trade> updateTradeTx(Trade trade) {

        return walletManager.getEscrowTransactionWithAmt(trade.getFundingTxHash())
                .map(ftx -> trade.copyBuilder().fundingTransactionWithAmt(ftx).build())
                .toSingle(trade)
                .flatMap(t -> walletManager.getEscrowTransactionWithAmt(t.getPayoutTxHash())
                        .map(ptx -> t.copyBuilder().payoutTransactionWithAmt(ptx).build())
                        .toSingle(t));
    }

    private Maybe<Trade> updateTrade(Trade trade, Trade receivedTrade) {

        TradeProtocol tradeProtocol = getProtocol(trade);

        Maybe<Trade> tradeUpdated = Maybe.empty();

        switch (trade.getStatus()) {

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

            default:
                break;
        }

        return tradeUpdated.map(Trade::withStatus);
    }

    private TradeProtocol getProtocol(Trade trade) {

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
}