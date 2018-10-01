package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.trade.model.TradeManagerException;
import com.bytabit.mobile.wallet.manager.WalletManager;
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
import java.util.Comparator;
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
                .doOnNext(ut -> log.debug("updatedTrade: {}", ut.getEscrowAddress()))
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

        Maybe<Boolean> walletsSynced = Observable.zip(walletManager.getTradeDownloadProgress().autoConnect(),
                walletManager.getEscrowDownloadProgress().autoConnect(), (tp, ep) -> tp == 1 && ep == 1)
                .filter(p -> p)
                .firstElement()
                .observeOn(Schedulers.io())
                .doOnSubscribe(d -> log.debug("walletSynced: subscribe"))
                .doOnSuccess(p -> log.debug("walletSynced: {}", p))
                .cache();

        // get stored trades after download progress is 100% loaded
        walletsSynced.flatMapObservable(p -> tradeStorage.getAll())
                .flatMapIterable(t -> t)
                .flatMapMaybe(t -> walletManager.getEscrowTransactionWithAmt(t.getFundingTxHash())
                        .map(txa -> t.copyBuilder().fundingTransactionWithAmt(txa).build())
                        .defaultIfEmpty(t))
                .flatMapMaybe(t -> walletManager.getEscrowTransactionWithAmt(t.getPayoutTxHash())
                        .map(txa -> t.copyBuilder().payoutTransactionWithAmt(txa).build())
                        .defaultIfEmpty(t))
                .map(Trade::withStatus)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(createdTradeSubject::onNext);

        // get update and store trades from received data after download progress is 100% loaded
        walletsSynced.flatMapSingle(p -> profileManager.loadOrCreateMyProfile())
                .flatMapObservable(profile -> Observable.interval(15, TimeUnit.SECONDS, Schedulers.io())
                        .flatMap(t -> tradeService.get(profile.getPubKey())
                                .doOnSuccess(l -> l.sort(Comparator.comparing(Trade::getVersion)))
                                .flattenAsObservable(l -> l))
                        .flatMapMaybe(trade -> handleReceivedTrade(profile, trade)))
                .flatMapSingle(tradeStorage::write)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> buyerCreateTrade(SellOffer sellOffer, BigDecimal btcAmount) {

        return buyerProtocol.createTrade(sellOffer, btcAmount)
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public ConnectableObservable<Trade> getCreatedTrade() {
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

    public Maybe<Trade> buyerSendPayment(String paymentReference) {
        return getLastSelectedTrade().autoConnect().firstOrError()
                .filter(trade -> trade.getStatus().equals(FUNDED))
                .flatMapSingleElement(this::updateTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(st -> buyerProtocol.sendPayment(st, paymentReference))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> sellerFundEscrow() {
        return getLastSelectedTrade().autoConnect().firstOrError()
                .filter(trade -> CREATED.equals(trade.getStatus()))
                .flatMap(st -> sellerProtocol.fundEscrow(st))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> sellerPaymentReceived() {
        return getLastSelectedTrade().autoConnect().firstOrError()
                .filter(trade -> PAID.equals(trade.getStatus()))
                .flatMapSingleElement(this::updateTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(st -> sellerProtocol.confirmPaymentReceived(st))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> requestArbitrate() {

        return getLastSelectedTrade().autoConnect().firstOrError()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) != 0)
                .filter(trade -> trade.getStatus().compareTo(CREATED) > 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) < 0)
                .flatMap(trade -> getProtocol(trade).requestArbitrate(trade))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> arbitratorRefundSeller() {

        return getLastSelectedTrade().autoConnect().firstOrError()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) == 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) == 0)
                .flatMapSingleElement(this::updateTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(trade -> arbitratorProtocol.refundSeller(trade))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> arbitratorPayoutBuyer() {

        return getLastSelectedTrade().autoConnect().firstOrError()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) == 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) == 0)
                .flatMapSingleElement(this::updateTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(trade -> arbitratorProtocol.payoutBuyer(trade))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> buyerRefundSeller() {
        return getLastSelectedTrade().autoConnect().firstOrError()
                .filter(trade -> trade.getRole().compareTo(BUYER) == 0)
                .filter(trade -> trade.getStatus().compareTo(FUNDED) == 0)
                .flatMapSingleElement(this::updateTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(trade -> buyerProtocol.refundTrade(trade))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    private Maybe<Trade> handleReceivedTrade(Profile profile, Trade receivedTrade) {

        Single<Trade> currentTrade = tradeStorage.read(receivedTrade.getEscrowAddress())
                .toSingle(createdFromReceivedTrade(receivedTrade))
                // add role
                .map(ct -> ct.withRole(profile.getPubKey()))
                // add trade tx
                .flatMap(this::updateTradeTx);

        Single<Trade> updatedReceivedTrade = Single.just(receivedTrade)
                .map(rt -> rt.withRole(profile.getPubKey()))
                // add trade tx
                .flatMap(this::updateTradeTx)
                // add status
                .map(Trade::withStatus);

        return Single.zip(currentTrade, updatedReceivedTrade, this::updateTrade)
                .flatMapMaybe(t -> t);
    }

    private Trade createdFromReceivedTrade(Trade receivedTrade) {

        // TODO validate escrowAddress, sellOffer, buyRequest
        String escrowAddress = walletManager.escrowAddress(receivedTrade.getArbitratorProfilePubKey(),
                receivedTrade.getSellerEscrowPubKey(),
                receivedTrade.getBuyerEscrowPubKey());

        return Trade.builder()
                .status(CREATED)
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

        Maybe<Trade> tradeUpdated;

        switch (trade.getStatus()) {

            case CREATED:
                tradeUpdated = tradeProtocol.handleCreated(trade, receivedTrade);
                break;

            case FUNDING:
                tradeUpdated = tradeProtocol.handleFunding(trade);
                break;

            case FUNDED:
                tradeUpdated = tradeProtocol.handleFunded(trade, receivedTrade);
                break;

            case CANCELING:
                tradeUpdated = tradeProtocol.handleCompleting(trade);
                break;

            case CANCELED:
                tradeUpdated = Maybe.empty();
                break;

            case PAID:
                tradeUpdated = tradeProtocol.handlePaid(trade, receivedTrade);
                break;

            case ARBITRATING:
                tradeUpdated = tradeProtocol.handleArbitrating(trade, receivedTrade);
                break;

            case COMPLETING:
                tradeUpdated = tradeProtocol.handleCompleting(trade);
                break;

            case COMPLETED:
                tradeUpdated = Maybe.empty();
                break;

            default:
                throw new TradeManagerException("Invalid status, can't update trade");
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