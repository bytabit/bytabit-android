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

package com.bytabit.mobile.offer.manager;

import com.bytabit.mobile.badge.manager.BadgeManager;
import com.bytabit.mobile.offer.model.Offer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.trade.manager.TradeManager;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
import io.reactivex.Flowable;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferManager {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final OfferService offerService;

    private final OfferStorage offerStorage;

    private final BehaviorSubject<Offer> selectedOfferSubject;

    @Inject
    WalletManager walletManager;

    @Inject
    TradeManager tradeManager;

    @Inject
    BadgeManager badgeManager;

    public OfferManager() {

        offerService = new OfferService();

        offerStorage = new OfferStorage();

        selectedOfferSubject = BehaviorSubject.create();
    }

    public Observable<Trade> getAddedTrades() {

        // get trades for offers I created
        return Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .flatMapSingle(t -> offerStorage.getAll())
                .flatMapIterable(ol -> ol)
                .flatMap(o -> tradeManager.addTradesCreatedFromOffer(o))
                .share();
    }

    public Observable<Offer> getUpdatedOffers() {

        // update my offers on the server so they don't get removed
        return Observable.interval(0, 150, TimeUnit.SECONDS, Schedulers.io())
                .flatMapSingle(t -> offerStorage.getAll())
                .flatMapIterable(ol -> ol)
                .flatMapMaybe(o -> offerService.put(o).toMaybe().onErrorResumeNext(Maybe.empty()));
    }

    public Single<List<Offer>> getStoredAndLoadedOffers() {
        return offerStorage.getAll().flattenAsObservable(o -> o)
                .concatWith(offerService.getAll().retryWhen(errors ->
                        errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS)))
                        .flattenAsObservable(o -> o)).distinct().toList();
    }

    public Observable<List<Offer>> getOffers() {
        return Observable.interval(0, 30, TimeUnit.SECONDS, Schedulers.io())
                .flatMapSingle(tick -> getStoredAndLoadedOffers());
    }

    public Maybe<Offer> createOffer(Offer.OfferType offerType,
                                    CurrencyCode currencyCode,
                                    PaymentMethod paymentMethod,
                                    BigDecimal minAmount,
                                    BigDecimal maxAmount,
                                    BigDecimal price) {

        if (offerType == null) {
            return Maybe.error(new OfferException("Offer type is required."));
        }
        if (currencyCode == null) {
            return Maybe.error(new OfferException("Currency code is required."));
        }
        if (paymentMethod == null) {
            return Maybe.error(new OfferException("Payment method is required."));
        }
        if (minAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Maybe.error(new OfferException("Minimum amount must be greater than zero."));
        }
        if (maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Maybe.error(new OfferException("Maximum amount must be greater than zero."));
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return Maybe.error(new OfferException("Price must be greater than zero."));
        }
        if (maxAmount.compareTo(currencyCode.getMaxTradeAmount()) > 0) {
            return Maybe.error(new OfferException(String.format("Max offer amount can not be more than %s %s.", currencyCode.getMaxTradeAmount(), currencyCode)));
        }

        return badgeManager.getOfferMakerBadge(currencyCode)
                .flatMapMaybe(b -> walletManager.getProfilePubKeyBase58().map(profilePubKey ->
                        Offer.builder()
                                .offerType(offerType)
                                .makerProfilePubKey(profilePubKey)
                                .currencyCode(currencyCode)
                                .paymentMethod(paymentMethod)
                                .minAmount(minAmount)
                                .maxAmount(maxAmount)
                                .price(price.setScale(currencyCode.getScale(), RoundingMode.HALF_UP))
                                .build())
                        .observeOn(Schedulers.io())
                        .flatMapSingleElement(offerStorage::write)
                        .flatMapSingleElement(offerService::put));
    }

    public Single<String> deleteOffer() {
        return getSelectedOffer()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .firstOrError().map(Offer::getId)
                .flatMap(offerService::delete)
                .map(Offer::getId)
                .flatMap(offerStorage::delete);
    }

    public void setSelectedOffer(Offer offer) {
        selectedOfferSubject.onNext(offer);
    }

    public Observable<Offer> getSelectedOffer() {
        return selectedOfferSubject
                .doOnSubscribe(d -> log.debug("selectedOffer: subscribe"))
                .doOnDispose(() -> log.debug("selectedOffer: dispose"))
                .doOnNext(o -> log.debug("selectedOffer: {}", o));
    }

    public Maybe<Trade> createTrade(BigDecimal btcAmount) {
        if (btcAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Maybe.error(new OfferException("Trade amount must be greater than zero."));
        }
        return getSelectedOffer().firstOrError()
                .flatMapMaybe(offer -> tradeManager.createTrade(offer, btcAmount));
    }
}