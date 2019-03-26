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
import com.bytabit.mobile.common.DateConverter;
import com.bytabit.mobile.offer.model.Offer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.trade.manager.TradeManager;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferManager {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final BehaviorSubject<Offer> selectedOfferSubject;

    private final PublishSubject<Offer> createdOffer;

    private final PublishSubject<Offer> removedOffer;

    private final OfferService offerService;

    private Observable<List<Offer>> offers;

    private final Gson gson;

    @Inject
    WalletManager walletManager;

    @Inject
    TradeManager tradeManager;

    @Inject
    BadgeManager badgeManager;

    OfferStorage offerStorage;

    public OfferManager() {

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Date.class, new DateConverter())
                .create();

        offerService = new OfferService();

        offerStorage = new OfferStorage();

        selectedOfferSubject = BehaviorSubject.create();

        createdOffer = PublishSubject.create();

        removedOffer = PublishSubject.create();
    }

    @PostConstruct
    public void initialize() {

        offers = Observable.interval(0, 30, TimeUnit.SECONDS, Schedulers.io())
                .flatMapSingle(tick -> getLoadedOffers());

        // get trades for offers I created
        Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .flatMap(t -> offerStorage.getAll())
                .flatMapIterable(ol -> ol)
                .flatMap(o -> tradeManager.addTradesCreatedFromOffer(o))
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(trade -> log.debug("added trade from my offer: {}", trade));

        // update my offers on the server so they don't get removed
        Observable.interval(0, 150, TimeUnit.SECONDS, Schedulers.io())
                .flatMap(t -> offerStorage.getAll())
                .flatMapIterable(ol -> ol)
                .flatMapMaybe(o -> offerService.put(o).toMaybe().onErrorResumeNext(Maybe.empty()))
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(trade -> log.debug("updated my offer: {}", trade));
    }

    public Single<List<Offer>> getLoadedOffers() {
        return offerStorage.getAll().flatMapIterable(o -> o)
                .concatWith(offerService.getAll().retryWhen(errors ->
                        errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS)))
                        .flattenAsObservable(o -> o)).distinct().toList();
    }

    public Observable<List<Offer>> getUpdatedOffers() {
        return offers.share();
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
                        .flatMapSingleElement(offerService::put)
                        .doOnSuccess(createdOffer::onNext));
    }

    public void deleteOffer() {
        getSelectedOffer().firstOrError().map(Offer::getId)
                .flatMap(offerStorage::delete)
                .flatMap(offerService::delete)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(removedOffer::onNext);
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

    public Observable<Offer> getCreatedOffer() {
        return createdOffer
                .doOnNext(offer -> log.debug("Created: {}", offer))
                .share();
    }

    public Observable<Offer> getRemovedOffer() {
        return removedOffer
                .doOnNext(id -> log.debug("Removed: {}", id))
                .share();
    }

    public Maybe<Trade> createTrade(BigDecimal btcAmount) {
        if (btcAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Maybe.error(new OfferException("Trade amount must be greater than zero."));
        }
        return getSelectedOffer().firstOrError()
                .flatMapMaybe(offer -> tradeManager.createTrade(offer, btcAmount));
    }

    public Single<String> getSelectedOfferAsJson() {
        return getSelectedOffer().firstOrError()
                .map(gson::toJson);
    }
}