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

package com.bytabit.app.core.trade;

import com.bytabit.app.core.arbitrate.ArbitratorManager;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.trade.model.CancelCompleted;
import com.bytabit.app.core.trade.model.PayoutCompleted;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.core.trade.model.TradeModelException;
import com.bytabit.app.core.wallet.WalletManager;
import com.bytabit.app.core.wallet.model.TransactionWithAmt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;

import static com.bytabit.app.core.offer.model.Offer.OfferType.BUY;
import static com.bytabit.app.core.offer.model.Offer.OfferType.SELL;
import static com.bytabit.app.core.trade.model.Trade.Role.ARBITRATOR;
import static com.bytabit.app.core.trade.model.Trade.Role.BUYER;
import static com.bytabit.app.core.trade.model.Trade.Role.SELLER;
import static com.bytabit.app.core.trade.model.Trade.Status.ACCEPTED;
import static com.bytabit.app.core.trade.model.Trade.Status.ARBITRATING;
import static com.bytabit.app.core.trade.model.Trade.Status.CANCELED;
import static com.bytabit.app.core.trade.model.Trade.Status.CANCELING;
import static com.bytabit.app.core.trade.model.Trade.Status.COMPLETED;
import static com.bytabit.app.core.trade.model.Trade.Status.COMPLETING;
import static com.bytabit.app.core.trade.model.Trade.Status.CREATED;
import static com.bytabit.app.core.trade.model.Trade.Status.FUNDED;
import static com.bytabit.app.core.trade.model.Trade.Status.FUNDING;
import static com.bytabit.app.core.trade.model.Trade.Status.PAID;

@Slf4j
@Singleton
public class TradeManager {

    private final WalletManager walletManager;

    private final ArbitratorManager arbitratorManager;

    private final SellerProtocol sellerProtocol;

    private final BuyerProtocol buyerProtocol;

    private final ArbitratorProtocol arbitratorProtocol;

    private final TradeService tradeService;

    private final TradeStorage tradeStorage;

    private final BehaviorSubject<Trade> selectedTradeSubject;

    @Inject
    public TradeManager(WalletManager walletManager, ArbitratorManager arbitratorManager,
                        SellerProtocol sellerProtocol, BuyerProtocol buyerProtocol,
                        ArbitratorProtocol arbitratorProtocol,
                        TradeService tradeService, TradeStorage tradeStorage) {

        this.walletManager = walletManager;
        this.arbitratorManager = arbitratorManager;
        this.sellerProtocol = sellerProtocol;
        this.buyerProtocol = buyerProtocol;
        this.arbitratorProtocol = arbitratorProtocol;
        this.tradeService = tradeService;
        this.tradeStorage = tradeStorage;

        selectedTradeSubject = BehaviorSubject.create();
    }

    private Single<Boolean> isArbitrator() {
        return walletManager.getProfilePubKey().firstOrError().map(profilePubKey -> {
            String arbitratorPubKey = arbitratorManager.getArbitrator().getPubkey();
            return profilePubKey.compareTo(arbitratorPubKey) == 0;
        });
    }

    public Observable<Trade> getUpdatedTrades() {

        Comparator<Trade> tradeVersionComparator = new Comparator<Trade>() {
            @Override
            public int compare(Trade o1, Trade o2) {
                return o1.getVersion().compareTo(o2.getVersion());
            }
        };

        // if arbitrator only get updated arbitrate trades
        Observable<Trade> updatedArbitrateTrades = isArbitrator().filter(a -> a)
                .flatMapObservable(a -> walletManager.getProfilePubKey()
                        .flatMap(profilePubKey -> Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                                .flatMapSingle(i -> tradeStorage.getAll().flattenAsObservable(t -> t).map(Trade::getVersion)
                                        .reduce(0L, (maxVersion, version) -> maxVersion.compareTo(version) >= 0 ? maxVersion : version))
                                .flatMapSingle(version -> tradeService.getArbitrate(version - 1).flattenAsObservable(t -> t).toSortedList(tradeVersionComparator))
                                .flatMapIterable(l -> l)
                                .flatMapMaybe(trade -> handleReceivedTrade(profilePubKey, trade))));

        // else if not arbitrator get stored trades and received updated trades
        Observable<Trade> updatedNonArbitrateTrades = isArbitrator().filter(a -> !a)
                .flatMapObservable(a -> walletManager.getProfilePubKey()
                        .flatMap(profilePubKey -> Observable.interval(15, TimeUnit.SECONDS, Schedulers.io())
                                .flatMapSingle(i -> tradeStorage.getAll())
                                .flatMapIterable(trades -> trades)
                                .flatMapSingle(trade -> tradeService.get(trade.getId(), trade.getVersion() - 1).flattenAsObservable(t -> t).toSortedList(tradeVersionComparator))
                                .flatMapIterable(l -> l)
                                .flatMapMaybe(trade -> handleReceivedTrade(profilePubKey, trade))));

        return updatedArbitrateTrades.mergeWith(updatedNonArbitrateTrades).flatMapSingle(tradeStorage::write)
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
                        .filter(t -> getStatus(t).equals(CREATED))
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
                        .map(this::withStatus)
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

    public Single<Trade> buyerSendPayment(String paymentReference) {

        if (paymentReference == null || paymentReference.length() == 0) {
            return Single.error(new TradeException("No payment reference."));

        }
        return getSelectedTrade().firstOrError()
                .filter(trade -> trade.getStatus().equals(FUNDED))
                .flatMapSingleElement(this::withTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMapSingle(st -> buyerProtocol.sendPayment(st, paymentReference))
                .map(this::withStatus)
                .flatMap(tradeStorage::write)
                .flatMap(tradeService::put);
    }

    public Single<Trade> fundEscrow() {
        return getSelectedTrade().firstOrError()
                .filter(trade -> ACCEPTED.equals(trade.getStatus()))
                .flatMapSingle(sellerProtocol::fundEscrow)
                .map(this::withStatus)
                .flatMap(tradeStorage::write)
                .flatMap(tradeService::put);
    }

    public Single<Trade> sellerPaymentReceived() {
        return getSelectedTrade().firstOrError()
                .filter(trade -> PAID.equals(trade.getStatus()))
                .flatMapSingleElement(this::withTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMapSingle(sellerProtocol::confirmPaymentReceived)
                .map(this::withStatus)
                .flatMap(tradeStorage::write)
                .flatMap(tradeService::put);
    }

    public Single<Trade> requestArbitrate() {

        return getSelectedTrade().firstOrError()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) != 0)
                .filter(trade -> trade.getStatus().compareTo(CREATED) > 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) < 0)
                .flatMapSingle(trade -> getProtocol(trade).requestArbitrate(trade))
                .map(this::withStatus)
                .flatMap(tradeStorage::write)
                .flatMap(tradeService::put);
    }

    public Maybe<Trade> arbitratorRefundSeller() {

        return getSelectedTrade().firstOrError()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) == 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) == 0)
                .flatMapSingleElement(this::withTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(arbitratorProtocol::refundSeller)
                .map(this::withStatus)
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put);
    }

    public Maybe<Trade> arbitratorPayoutBuyer() {

        return getSelectedTrade().firstOrError()
                .filter(trade -> trade.getRole().compareTo(ARBITRATOR) == 0)
                .filter(trade -> trade.getStatus().compareTo(ARBITRATING) == 0)
                .flatMapSingleElement(this::withTradeTx)
                .filter(trade -> trade.getFundingTransactionWithAmt() != null)
                .flatMap(arbitratorProtocol::payoutBuyer)
                .map(this::withStatus)
                .flatMapSingleElement(tradeStorage::write)
                .flatMapSingleElement(tradeService::put);
    }

    public Single<Trade> cancelTrade() {

        Single<Trade> trade = getSelectedTrade().firstOrError();

        Single<Trade> canceledTrade = trade.flatMap(t -> {

            if (t.getRole().compareTo(SELLER) == 0 && !t.hasPaymentRequest()) {
                return sellerProtocol.cancelUnfundedTrade(t);
            } else if (t.getRole().compareTo(BUYER) == 0 && !t.hasPaymentRequest()) {
                return buyerProtocol.cancelUnfundedTrade(t);
            } else if (t.getRole().compareTo(BUYER) == 0 && t.hasPaymentRequest()) {
                return withTradeTx(t).filter(t2 -> t2.getFundingTransactionWithAmt() != null)
                        .flatMapSingle(buyerProtocol::cancelFundingTrade);
            } else {
                return Single.error(new TradeException("Can't cancel trade."));
            }
        });

        return canceledTrade.flatMap(tradeStorage::write).flatMap(tradeService::put);
    }

    private Maybe<Trade> handleReceivedTrade(String profilePubKey, Trade receivedTrade) {

        Single<Trade> currentTrade = tradeStorage.read(receivedTrade.getId())
                .toSingle(createdFromReceivedTrade(profilePubKey, receivedTrade))
                // add role
                .map(t -> withRole(profilePubKey, t))
                // add trade tx
                .flatMap(this::withTradeTx);

        Single<Trade> updatedReceivedTrade = Single.just(receivedTrade)
                // add role
                .map(t -> withRole(profilePubKey, t))
                // add trade tx
                .flatMap(this::withTradeTx);

        return Single.zip(currentTrade, updatedReceivedTrade, this::updateTrade)
                .flatMapMaybe(t -> t);
    }

    private Trade withRole(String profilePubKey, Trade trade) {
        trade.setRole(getRole(profilePubKey, trade));
        return trade;
    }

    private Trade.Role getRole(String profilePubKey, Trade trade) {

        final Trade.Role role;

        if (SELL.equals(trade.getOffer().getOfferType()) && trade.getMakerProfilePubKey().equals(profilePubKey)) {
            role = SELLER;
        } else if (BUY.equals(trade.getOffer().getOfferType()) && trade.getMakerProfilePubKey().equals(profilePubKey)) {
            role = BUYER;
        } else if (SELL.equals(trade.getOffer().getOfferType()) && trade.getTakerProfilePubKey().equals(profilePubKey)) {
            role = BUYER;
        } else if (BUY.equals(trade.getOffer().getOfferType()) && trade.getTakerProfilePubKey().equals(profilePubKey)) {
            role = SELLER;
        } else if (trade.hasAcceptance() && trade.getArbitratorProfilePubKey().equals(profilePubKey)) {
            role = ARBITRATOR;
        } else {
            throw new TradeModelException("Unable to determine trade role.");
        }
        return role;
    }

    private Trade withStatus(Trade trade) {
        trade.setStatus(getStatus(trade));
        return trade;
    }

    private Trade.Status getStatus(Trade trade) {

        Trade.Status status = null;
        if (trade.hasOffer() && trade.hasTakeOfferRequest()) {
            status = CREATED;
        }
        if (status == CREATED && trade.hasAcceptance()) {
            status = ACCEPTED;
        }
        if (status == ACCEPTED && trade.hasPaymentRequest()) {
            status = FUNDING;
        }
        if (status == FUNDING && trade.getFundingTransactionWithAmt() != null && trade.getFundingTransactionWithAmt().getDepth() > 0) {
            status = FUNDED;
        }
        if (status == FUNDED && trade.hasPayoutRequest()) {
            status = PAID;
        }
        if (status == FUNDED && trade.hasPayoutCompleted() && trade.getPayoutCompleted().getReason().equals(PayoutCompleted.Reason.BUYER_SELLER_REFUND)) {
            status = COMPLETING;
        }
        if (trade.hasArbitrateRequest()) {
            status = ARBITRATING;
        }
        if ((status == PAID || status == ARBITRATING || status == CANCELING) && trade.getPayoutTxHash() != null) {
            status = COMPLETING;
        }
        if (status == COMPLETING && trade.getPayoutTransactionWithAmt() != null && trade.getPayoutTransactionWithAmt().getDepth() > 0) {
            status = COMPLETED;
        }
        if ((status == CREATED || status == ACCEPTED) && trade.hasCancelCompleted() &&
                (trade.getCancelCompleted().getReason().equals(CancelCompleted.Reason.SELLER_CANCEL_UNFUNDED) ||
                        trade.getCancelCompleted().getReason().equals(CancelCompleted.Reason.BUYER_CANCEL_UNFUNDED))) {
            status = CANCELED;
        }
        if ((status == FUNDING || status == FUNDED) && trade.hasCancelCompleted() &&
                trade.getCancelCompleted().getReason().equals(CancelCompleted.Reason.BUYER_CANCEL_FUNDED)) {
            status = CANCELING;
        }
        if (status == CANCELING && trade.getPayoutTransactionWithAmt() != null && trade.getPayoutTransactionWithAmt().getDepth() > 0) {
            status = CANCELED;
        }

        if (status == null) {
            throw new TradeModelException("Unable to determine trade status.");
        }
        return status;
    }

    private Trade createdFromReceivedTrade(String profilePubKey, Trade receivedTrade) {

        // TODO validate offer, tradeRequest

        return Trade.builder()
                .id(receivedTrade.getId())
                .status(CREATED)
                .role(getRole(profilePubKey, receivedTrade))
                .createdTimestamp(new Date())
                .offer(receivedTrade.getOffer())
                .tradeRequest(receivedTrade.getTradeRequest())
                .tradeAcceptance(receivedTrade.getTradeAcceptance())
                .build();
    }

    private Single<Trade> withTradeTx(Trade trade) {

        return walletManager.getEscrowTransactionWithAmt(trade.getFundingTxHash())
                .map(ftx -> withFundingTransaction(trade, ftx))
                .toSingle(trade)
                .flatMap(t -> walletManager.getEscrowTransactionWithAmt(t.getPayoutTxHash())
                        .map(ptx -> withPayoutTransaction(t, ptx)).toSingle(t));
    }

    private Trade withFundingTransaction(Trade trade, TransactionWithAmt ftx) {
        trade.setFundingTransactionWithAmt(ftx);
        return trade;
    }

    private Trade withPayoutTransaction(Trade trade, TransactionWithAmt ptx) {
        trade.setPayoutTransactionWithAmt(ptx);
        return trade;
    }

    private Maybe<Trade> updateTrade(Trade trade, Trade receivedTrade) {

        //log.debug("updateTrade:\ntrade version={} roll={} status={}\nreceivedTrade version={} roll={} status={}", trade.getVersion(), trade.getRole(), trade.getStatus(), receivedTrade.getVersion(), receivedTrade.getRole(), receivedTrade.getStatus());

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

        return tradeUpdated.map(this::withStatus);
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