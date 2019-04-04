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

import com.bytabit.mobile.offer.model.Offer;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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

    private final BehaviorSubject<Trade> selectedTradeSubject;

    public TradeManager() {

        tradeService = new TradeService();

        tradeStorage = new TradeStorage();

        selectedTradeSubject = BehaviorSubject.create();
    }

    public Observable<Trade> getUpdatedTrades() {

        Comparator<Trade> tradeVersionComparator = new Comparator<Trade>() {
            @Override
            public int compare(Trade o1, Trade o2) {
                return o1.getVersion().compareTo(o2.getVersion());
            }
        };

        // get update and store trades from received data after download started
        return walletManager.getProfilePubKey()
                .flatMap(profilePubKey -> Observable.interval(15, TimeUnit.SECONDS, Schedulers.io())
                        .flatMapSingle(i -> tradeStorage.getAll())
                        .flatMapIterable(trades -> trades)
                        .flatMapSingle(trade -> tradeService.get(trade.getId(), trade.getVersion() - 1).flattenAsObservable(t -> t).toSortedList(tradeVersionComparator))
                        .flatMapIterable(l -> l)
                        .flatMapMaybe(trade -> handleReceivedTrade(profilePubKey, trade)))
                .flatMapSingle(tradeStorage::write)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io());
    }

    public Maybe<Trade> createTrade(Offer offer, BigDecimal btcAmount) {
        Maybe<Trade> trade = Maybe.empty();

        BigDecimal currencyAmount = offer.getPrice().multiply(btcAmount).setScale(offer.getCurrencyCode().getScale(), RoundingMode.HALF_UP);
        if (currencyAmount.compareTo(offer.getMinAmount()) < 0) {
            return Maybe.error(new TradeException(String.format("Trade amount can not be less than %s %s.", offer.getMinAmount(), offer.getCurrencyCode())));
        }
        if (currencyAmount.compareTo(offer.getMaxAmount()) > 0 || currencyAmount.compareTo(offer.getCurrencyCode().getMaxTradeAmount()) > 0) {
            return Maybe.error(new TradeException(String.format("Trade amount can not be more than %s %s.", offer.getMaxAmount(), offer.getCurrencyCode())));
        }

        if (SELL.equals(offer.getOfferType())) {
            trade = buyerProtocol.createTrade(offer, btcAmount)
                    .flatMapSingleElement(tradeStorage::write)
                    .flatMapSingleElement(tradeService::put);
        } else if (BUY.equals(offer.getOfferType())) {
            trade = sellerProtocol.createTrade(offer, btcAmount)
                    .flatMapSingleElement(tradeStorage::write)
                    .flatMapSingleElement(tradeService::put);
        }
        return trade;
    }

    public Observable<Trade> addTradesCreatedFromOffer(Offer offer) {

        return walletManager.getProfilePubKey()
                .flatMap(profilePubKey -> tradeService.getByOfferId(offer.getId(), 0L)
                        .flattenAsObservable(l -> l)
                        .filter(t -> t.getMakerProfilePubKey().equals(profilePubKey))
                        .map(Trade::withStatus)
                        .filter(t -> t.getStatus().equals(CREATED))
                        .flatMapMaybe(trade -> handleReceivedTrade(profilePubKey, trade))
                        .flatMapSingle(tradeStorage::write)
                        .observeOn(Schedulers.io())
                        .subscribeOn(Schedulers.io()));
    }

    public Single<List<Trade>> getStoredTrades() {

        Observable<Boolean> walletsRunning = walletManager.getWalletsRunning()
                .filter(s -> s)
                .observeOn(Schedulers.io())
                .doOnSubscribe(d -> log.debug("walletsRunning: subscribe"))
                .doOnNext(p -> log.debug("walletsRunning: {}", p));

        // get stored trades after download started
        return walletsRunning.filter(r -> r)
                .flatMapSingle(r -> tradeStorage.getAll()
                        .flattenAsObservable(t -> t)
                        .flatMapMaybe(t -> walletManager.getEscrowTransactionWithAmt(t.getFundingTxHash())
                                .map(txa -> t.copyBuilder().fundingTransactionWithAmt(txa).build())
                                .defaultIfEmpty(t))
                        .flatMapMaybe(t -> walletManager.getEscrowTransactionWithAmt(t.getPayoutTxHash())
                                .map(txa -> t.copyBuilder().payoutTransactionWithAmt(txa).build())
                                .defaultIfEmpty(t))
                        .map(Trade::withStatus)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .toList()
                        .doOnSubscribe(d -> log.debug("storedTrades: subscribe"))
                        .doOnSuccess(l -> log.debug("got storedTrades: {}", l)))
                .firstOrError();
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

        if (paymentReference == null || paymentReference.length() == 0) {
            return Maybe.error(new TradeException("No payment reference."));

        }
        return getSelectedTrade().firstOrError()
                .filter(trade -> trade.getStatus().equals(FUNDED))
                .flatMapSingleElement(this::updateTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(st -> buyerProtocol.sendPayment(st, paymentReference))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put);
    }

    public Maybe<Trade> fundEscrow() {
        return getSelectedTrade().firstOrError()
                .filter(trade -> ACCEPTED.equals(trade.getStatus()))
                .flatMap(st -> sellerProtocol.fundEscrow(st))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put);
    }

    public Maybe<Trade> sellerPaymentReceived() {
        return getSelectedTrade().firstOrError()
                .filter(trade -> PAID.equals(trade.getStatus()))
                .flatMapSingleElement(this::updateTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(st -> sellerProtocol.confirmPaymentReceived(st))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put);
    }

    public Maybe<Trade> requestArbitrate() {

        return getSelectedTrade().firstOrError()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) != 0)
                .filter(trade -> trade.getStatus().compareTo(CREATED) > 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) < 0)
                .flatMap(trade -> getProtocol(trade).requestArbitrate(trade))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put);
    }

    public Maybe<Trade> arbitratorRefundSeller() {

        return getSelectedTrade().firstOrError()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) == 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) == 0)
                .flatMapSingleElement(this::updateTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(trade -> arbitratorProtocol.refundSeller(trade))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put);
    }

    public Maybe<Trade> arbitratorPayoutBuyer() {

        return getSelectedTrade().firstOrError()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) == 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) == 0)
                .flatMapSingleElement(this::updateTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(trade -> arbitratorProtocol.payoutBuyer(trade))
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put);
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
                .flatMapSingleElement(tradeService::put);
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
                .createdTimestamp(new Date())
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
                return Maybe.error(new TradeException("Invalid status, can't update trade"));
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
            throw new TradeException("Unable to determine trade protocol.");
        }
    }
}