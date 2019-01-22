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

package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.common.LocalDateTimeConverter;
import com.bytabit.mobile.offer.model.Offer;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.trade.model.TradeManagerException;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.bytabit.mobile.offer.model.Offer.OfferType.BUY;
import static com.bytabit.mobile.offer.model.Offer.OfferType.SELL;
import static com.bytabit.mobile.trade.model.Trade.Role.*;
import static com.bytabit.mobile.trade.model.Trade.Status.*;

public class TradeManager {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    WalletManager walletManager;

    @Inject
    SellerProtocol sellerProtocol;

    @Inject
    BuyerProtocol buyerProtocol;

    @Inject
    ArbitratorProtocol arbitratorProtocol;

    private final TradeService tradeService;

    private final TradeStorage tradeStorage;

    private final ReplaySubject<Trade> createdTradeSubject;

    private final ConnectableObservable<Trade> createdTrade;

    private final BehaviorSubject<Trade> updatedTradeSubject;

    private final Observable<Trade> updatedTrade;

    private final BehaviorSubject<Trade> selectedTradeSubject;

    private final Observable<Long> maxVersion;

    private final Gson gson;

    public TradeManager() {

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(ZonedDateTime.class, new LocalDateTimeConverter())
                .create();

        tradeService = new TradeService();

        tradeStorage = new TradeStorage();

        createdTradeSubject = ReplaySubject.create();

        createdTrade = createdTradeSubject
                .doOnSubscribe(d -> log.debug("createdTrade: subscribe"))
                .doOnNext(ct -> log.debug("createdTrade: {}", ct.getId()))
                .replay();

        updatedTradeSubject = BehaviorSubject.create();

        updatedTrade = updatedTradeSubject
                .doOnSubscribe(d -> log.debug("updatedTrade: subscribe"))
                .doOnNext(ut -> log.debug("updatedTrade: {}", ut.getId()))
                .share();

        selectedTradeSubject = BehaviorSubject.create();

        maxVersion = updatedTradeSubject
                .doOnSubscribe(d -> log.debug("maxVersion updatedTradeSubject: subscribe"))
                .doOnNext(p -> log.debug("maxVersion updatedTradeSubject: {}", p))
                .map(Trade::getVersion)
                .scan(0L, Long::max)
                .replay(1).autoConnect();
    }

    @PostConstruct
    public void initialize() {

        tradeStorage.createTradesDir();

        Observable<Boolean> walletsRunning = walletManager.getWalletsRunning()
                .filter(s -> s)
                .observeOn(Schedulers.io())
                .doOnSubscribe(d -> log.debug("walletsRunning: subscribe"))
                .doOnNext(p -> log.debug("walletsRunning: {}", p))
                .replay(1).autoConnect();

        // get stored trades after download started
        walletsRunning.flatMap(isRunning -> tradeStorage.getAll())
                .flatMapIterable(t -> t)
                .flatMapMaybe(t -> walletManager.getEscrowTransactionWithAmt(t.getFundingTxHash())
                        .map(txa -> t.copyBuilder().fundingTransactionWithAmt(txa).build())
                        .defaultIfEmpty(t))
                .flatMapMaybe(t -> walletManager.getEscrowTransactionWithAmt(t.getPayoutTxHash())
                        .map(txa -> t.copyBuilder().payoutTransactionWithAmt(txa).build())
                        .defaultIfEmpty(t))
                .map(Trade::withStatus)
                .doOnNext(t -> log.debug("created trade: {}", t))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(createdTradeSubject::onNext);

        Observable<Set<String>> activeTradeIds = Observable.merge(createdTradeSubject, updatedTradeSubject)
                .doOnSubscribe(d -> log.debug("activeTradeIds updatedTradeSubject: subscribe"))
                .doOnNext(p -> log.debug("activeTradeIds updatedTradeSubject: {}", p))
                .scan((Set<String>) new HashSet<String>(), (ts, t) -> {
                    if (t.getStatus().compareTo(COMPLETED) < 0) {
                        ts.add(t.getId());
                    } else {
                        ts.remove(t.getId());
                    }
                    return ts;
                })
                .replay(1).autoConnect();

        // get update and store trades from received data after download started
        walletManager.getProfilePubKey()
                .flatMap(profilePubKey -> Observable.interval(15, TimeUnit.SECONDS, Schedulers.io())
                        .flatMap(t -> maxVersion.firstOrError().flatMapObservable(version -> activeTradeIds.firstElement()
                                .flatMapSingle(tids -> tradeService.get(tids, version - 1))
                                .doOnSuccess(l -> l.sort(Comparator.comparing(Trade::getVersion)))
                                .flattenAsObservable(l -> l))
                                .flatMapMaybe(trade -> handleReceivedTrade(profilePubKey, trade))))
                .flatMapSingle(tradeStorage::write)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> createTrade(Offer offer, BigDecimal btcAmount) {
        Maybe<Trade> trade = Maybe.empty();

        if (SELL.equals(offer.getOfferType())) {
            trade = buyerProtocol.createTrade(offer, btcAmount)
                    .flatMapSingleElement(tradeStorage::write)
                    .flatMapSingleElement(tradeService::put)
                    .doOnSuccess(updatedTradeSubject::onNext);
        } else if (BUY.equals(offer.getOfferType())) {
            trade = sellerProtocol.createTrade(offer, btcAmount)
                    .flatMapSingleElement(tradeStorage::write)
                    .flatMapSingleElement(tradeService::put)
                    .doOnSuccess(updatedTradeSubject::onNext);
        }
        return trade;
    }

    public Observable<Trade> addTradesCreatedFromOffer(String profilePubKey, Offer offer) {

        return tradeService.getByOfferId(offer.getId(), 0L)
                .flattenAsObservable(l -> l)
                .filter(t -> t.getMakerProfilePubKey().equals(profilePubKey))
                .map(Trade::withStatus)
                .filter(t -> t.getStatus().equals(CREATED))
                .flatMapMaybe(trade -> handleReceivedTrade(profilePubKey, trade))
                .flatMapSingle(tradeStorage::write)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .doOnNext(updatedTradeSubject::onNext);
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

    public Observable<Trade> getSelectedTrade() {
        return selectedTradeSubject
                .doOnSubscribe(d -> log.debug("selectedTrade: subscribe"))
                .doOnDispose(() -> log.debug("selectedTrade: dispose"))
                .doOnNext(t -> log.debug("selectedTrade: {}", t));
    }

    public Maybe<Trade> buyerSendPayment(String paymentReference) {
        return getSelectedTrade().firstOrError()
                .filter(trade -> trade.getStatus().equals(FUNDED))
                .flatMapSingleElement(this::updateTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(st -> buyerProtocol.sendPayment(st, paymentReference))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> fundEscrow() {
        return getSelectedTrade().firstOrError()
                .filter(trade -> ACCEPTED.equals(trade.getStatus()))
                .flatMap(st -> sellerProtocol.fundEscrow(st))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> sellerPaymentReceived() {
        return getSelectedTrade().firstOrError()
                .filter(trade -> PAID.equals(trade.getStatus()))
                .flatMapSingleElement(this::updateTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(st -> sellerProtocol.confirmPaymentReceived(st))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> requestArbitrate() {

        return getSelectedTrade().firstOrError()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) != 0)
                .filter(trade -> trade.getStatus().compareTo(CREATED) > 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) < 0)
                .flatMap(trade -> getProtocol(trade).requestArbitrate(trade))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> arbitratorRefundSeller() {

        return getSelectedTrade().firstOrError()
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

        return getSelectedTrade().firstOrError()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) == 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) == 0)
                .flatMapSingleElement(this::updateTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(trade -> arbitratorProtocol.payoutBuyer(trade))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public Maybe<Trade> cancelTrade() {

        Single<Trade> trade = getSelectedTrade().firstOrError();

        Maybe<Trade> sellerCancelUnfunded = trade
                .filter(t -> t.getRole().compareTo(SELLER) == 0)
                .filter(t -> !t.hasPaymentRequest())
                .flatMap(t -> sellerProtocol.cancelUnfundedTrade(t));

        Maybe<Trade> buyerCancelUnfunded = trade
                .filter(t -> t.getRole().compareTo(BUYER) == 0)
                .filter(t -> !t.hasPaymentRequest())
                .flatMap(t -> buyerProtocol.cancelUnfundedTrade(t));

        Maybe<Trade> buyerCancelFunding = trade
                .filter(t -> t.getRole().compareTo(BUYER) == 0)
                .filter(Trade::hasPaymentRequest)
                .flatMapSingleElement(this::updateTradeTx)
                .filter(t -> t.getFundingTransactionWithAmt() != null)
                .flatMap(t -> buyerProtocol.cancelFundingTrade(t));

        return Maybe.concat(sellerCancelUnfunded, buyerCancelUnfunded, buyerCancelFunding)
                .lastElement()
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put)
                .doOnSuccess(updatedTradeSubject::onNext);
    }

    public Single<String> getSelectedTradeAsJson() {
        return getSelectedTrade().firstOrError()
                .map(gson::toJson);
    }

    private Maybe<Trade> handleReceivedTrade(String profilePubKey, Trade receivedTrade) {

        Single<Trade> currentTrade = tradeStorage.read(receivedTrade.getId())
                .toSingle(createdFromReceivedTrade(receivedTrade))
                // add role
                .map(ct -> ct.withRole(profilePubKey))
                // add trade tx
                .flatMap(this::updateTradeTx);

        Single<Trade> updatedReceivedTrade = Single.just(receivedTrade)
                .map(rt -> rt.withRole(profilePubKey))
                // add trade tx
                .flatMap(this::updateTradeTx)
                // add status
                .map(Trade::withStatus);

        return Single.zip(currentTrade, updatedReceivedTrade, this::updateTrade)
                .flatMapMaybe(t -> t);
    }

    private Trade createdFromReceivedTrade(Trade receivedTrade) {

        // TODO validate offer, tradeRequest

        return Trade.builder()
                .status(CREATED)
                .createdTimestamp(ZonedDateTime.now())
                .offer(receivedTrade.getOffer())
                .tradeRequest(receivedTrade.getTradeRequest())
                .tradeAcceptance(receivedTrade.getTradeAcceptance())
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

            case ACCEPTED:
                tradeUpdated = tradeProtocol.handleAccepted(trade, receivedTrade);
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
                tradeUpdated = tradeProtocol.handleCanceled(trade, receivedTrade);
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