package com.bytabit.mobile.offer.manager;

import com.bytabit.mobile.arbitrate.manager.ArbitratorManager;
import com.bytabit.mobile.common.LocalDateTimeConverter;
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
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferManager {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PublishSubject<Offer> selectedOfferSubject;

    private final Observable<Offer> selectedOffer;

    private final ConnectableObservable<Offer> lastSelectedOffer;

    private final PublishSubject<Offer> createdOffer;

    private final PublishSubject<Offer> removedOffer;

    private final OfferService sellOfferService;

    private Observable<List<Offer>> offers;

    private final Gson gson;

    @Inject
    ArbitratorManager arbitratorManager;

    @Inject
    WalletManager walletManager;

    @Inject
    TradeManager tradeManager;

    public OfferManager() {

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(ZonedDateTime.class, new LocalDateTimeConverter())
                .create();

        sellOfferService = new OfferService();

        selectedOfferSubject = PublishSubject.create();

        selectedOffer = selectedOfferSubject.share();

        lastSelectedOffer = selectedOffer.replay(1);

        createdOffer = PublishSubject.create();

        removedOffer = PublishSubject.create();
    }

    @PostConstruct
    public void initialize() {

        offers = Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .flatMap(tick -> getLoadedOffers());
    }

    public Observable<List<Offer>> getLoadedOffers() {
        return sellOfferService.getAll().retryWhen(errors ->
                errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS)))
                .toObservable();
    }

    public Observable<List<Offer>> getUpdatedOffers() {
        return offers.share();
    }

    public void createOffer(Offer.OfferType offerType, CurrencyCode currencyCode, PaymentMethod paymentMethod,
                            BigDecimal minAmount, BigDecimal maxAmount, BigDecimal price) {

        Maybe.zip(walletManager.getProfilePubKeyBase58(), walletManager.getEscrowPubKeyBase58(), (ppk, epk) ->
                Offer.builder()
                        .offerType(offerType)
                        .traderProfilePubKey(ppk)
                        .traderEscrowPubKey(epk)
                        .arbitratorProfilePubKey(arbitratorManager.getArbitrator().getPubkey())
                        .currencyCode(currencyCode)
                        .paymentMethod(paymentMethod)
                        .minAmount(minAmount)
                        .maxAmount(maxAmount)
                        .price(price.setScale(currencyCode.getScale(), RoundingMode.HALF_UP))
                        .build()
        )
                .flatMapSingle(sellOfferService::put)
                .subscribe(createdOffer::onNext);
    }

    public void deleteOffer() {
        selectedOffer.map(Offer::getTraderEscrowPubKey)
                .flatMapSingle(sellOfferService::delete)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(removedOffer::onNext);
    }

    public void setSelectedOffer(Offer sellOffer) {
        selectedOfferSubject.onNext(sellOffer);
    }

    public Observable<Offer> getSelectedOffer() {
        return selectedOffer
                .doOnNext(sellOffer -> log.debug("Selected: {}", sellOffer));
    }

    public ConnectableObservable<Offer> getLastSelectedOffer() {
        return lastSelectedOffer;
    }

    public Observable<Offer> getCreatedOffer() {
        return createdOffer
                .doOnNext(sellOffer -> log.debug("Created: {}", sellOffer))
                .share();
    }

    public Observable<Offer> getRemovedOffer() {
        return removedOffer
                .doOnNext(sellOffer -> log.debug("Removed: {}", sellOffer))
                .share();
    }

    public Maybe<Trade> createTrade(BigDecimal btcAmount) {
        return getLastSelectedOffer().autoConnect().firstOrError()
                .flatMapMaybe(sellOffer -> tradeManager.buyerCreateTrade(sellOffer, btcAmount));
    }

    public Single<String> getSelectedOfferAsJson() {
        return getLastSelectedOffer().autoConnect().firstOrError()
                .map(gson::toJson);
    }
}