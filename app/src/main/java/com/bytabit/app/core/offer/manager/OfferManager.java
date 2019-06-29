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

package com.bytabit.app.core.offer.manager;

import com.bytabit.app.core.badge.manager.BadgeManager;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.payment.model.CurrencyCode;
import com.bytabit.app.core.payment.model.PaymentMethod;
import com.bytabit.app.core.trade.manager.TradeManager;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.core.wallet.manager.WalletManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class OfferManager {

    private final OfferService offerService;

    private final OfferStorage offerStorage;

    private final BehaviorSubject<Offer> selectedOfferSubject;

    private final WalletManager walletManager;

    private final TradeManager tradeManager;

    private final BadgeManager badgeManager;

    @Inject
    public OfferManager(WalletManager walletManager, TradeManager tradeManager,
                        BadgeManager badgeManager, OfferService offerService, OfferStorage offerStorage) {

        this.walletManager = walletManager;
        this.tradeManager = tradeManager;
        this.badgeManager = badgeManager;
        this.offerService = offerService;
        this.offerStorage = offerStorage;
        selectedOfferSubject = BehaviorSubject.create();
    }

    public Observable<Trade> getAddedTrades() {

        // get trades for offers I created
        return Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .flatMapSingle(t -> offerStorage.getAll())
                .flatMapIterable(ol -> ol)
                .flatMap(tradeManager::addTradesCreatedFromOffer)
                .share();
    }

    // update my offers on the server so they don't get removed
    public Observable<Offer> getUpdatedOffers() {
        return Observable.interval(0, 5, TimeUnit.MINUTES, Schedulers.io())
                .flatMapSingle(t -> offerStorage.getAll())
                .flatMapIterable(ol -> ol)
                .flatMapMaybe(o -> offerService.put(o).toMaybe().onErrorResumeNext(Maybe.empty()));
    }

    public Observable<List<Offer>> getOffers() {
        return Observable.interval(0, 30, TimeUnit.SECONDS, Schedulers.io())
                .flatMapSingle(tick -> getStoredAndLoadedOffers()).replay(1).autoConnect();
    }

    // get offers from storage and server, filter out any that have invalid signatures
    private Single<List<Offer>> getStoredAndLoadedOffers() {
        return offerStorage.getAll().flattenAsObservable(o -> o)
                .concatWith(offerService.getAll().retryWhen(errors ->
                        errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS)))
                        .flattenAsObservable(o -> o))
                .distinct(Offer::getId)
                .withLatestFrom(walletManager.getProfilePubKeyBase58().toObservable(), (o, pubKey) -> {
                    o.setIsMine(o.getMakerProfilePubKey().equals(pubKey));
                    return o;
                })
                .toList();
    }

    public Single<Offer> createOffer(Offer.OfferType offerType,
                                     CurrencyCode currencyCode,
                                     PaymentMethod paymentMethod,
                                     BigDecimal minAmount,
                                     BigDecimal maxAmount,
                                     BigDecimal price) {

        if (offerType == null) {
            return Single.error(new OfferException("Offer type is required."));
        }
        if (currencyCode == null) {
            return Single.error(new OfferException("Currency code is required."));
        }
        if (paymentMethod == null) {
            return Single.error(new OfferException("Payment method is required."));
        }
        if (minAmount.compareTo(currencyCode.getMinTradeAmount()) < 0) {
            return Single.error(new OfferException(String.format("Minimum amount must be greater than %s %s.", currencyCode.getMinTradeAmount(), currencyCode)));
        }
        if (maxAmount.compareTo(currencyCode.getMinTradeAmount()) < 0) {
            return Single.error(new OfferException("Maximum amount must be greater than or equal to minimum amount."));
        }
        if (maxAmount.compareTo(currencyCode.getMaxTradeAmount()) > 0) {
            return Single.error(new OfferException(String.format("Max offer amount can not be more than %s %s.", currencyCode.getMaxTradeAmount(), currencyCode)));
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return Single.error(new OfferException("Price must be greater than zero."));
        }

        return badgeManager.getOfferMakerBadge(currencyCode)
                .flatMap(b -> walletManager.getProfilePubKeyBase58().map(profilePubKey ->
                        Offer.builder()
                                .offerType(offerType)
                                .makerProfilePubKey(profilePubKey)
                                .currencyCode(currencyCode)
                                .paymentMethod(paymentMethod)
                                .minAmount(minAmount)
                                .maxAmount(maxAmount)
                                .price(price.setScale(currencyCode.getScale(), RoundingMode.HALF_UP))
                                .build()).toSingle()
                )
                .observeOn(Schedulers.io())
                .flatMap(offerStorage::write)
                .flatMap(offerService::put);
    }

    public Single<String> deleteOffer() {
        return getSelectedOffer()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .firstOrError().map(Offer::getId)
                .flatMap(offerStorage::delete)
                .flatMap(offerService::delete)
                .map(Offer::getId);
    }

    public void setSelectedOffer(Offer offer) {
        selectedOfferSubject.onNext(offer);
    }

    public Observable<Offer> getSelectedOffer() {
        return selectedOfferSubject
                .doOnSubscribe(d -> log.debug("getSelectedOffer: subscribe"))
                .doOnDispose(() -> log.debug("getSelectedOffer: dispose"))
                .doOnNext(o -> log.debug("getSelectedOffer: {}", o));
    }

    public Maybe<Trade> createTrade(BigDecimal btcAmount) {
        if (btcAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Maybe.error(new OfferException("Trade amount must be greater than zero."));
        }
        return getSelectedOffer().firstOrError()
                .flatMapMaybe(offer -> tradeManager.createTrade(offer, btcAmount));
    }
}