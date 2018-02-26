package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(OfferManager.class);

    private final SellOfferService sellOfferService;

    @Inject
    ProfileManager profileManager;

    @Inject
    WalletManager walletManager;

    private final Observable<List<SellOffer>> offers;

    private final PublishSubject<SellOffer> selectedOfferSubject;

    private final Observable<SellOffer> selectedOffer;

    public OfferManager() {
        Retrofit sellOfferRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(new JacksonJrConverter<>(SellOffer.class))
                .build();

        sellOfferService = sellOfferRetrofit.create(SellOfferService.class);

        offers = Observable.concat(singleOffers().toObservable(), observableOffers())
                .subscribeOn(Schedulers.io()).publish().autoConnect();

        selectedOfferSubject = PublishSubject.create();
        selectedOffer = selectedOfferSubject.publish().autoConnect()
                .subscribeOn(Schedulers.io());
    }

    public Single<SellOffer> createOffer(CurrencyCode currencyCode, PaymentMethod paymentMethod, Profile arbitrator,
                                         BigDecimal minAmount, BigDecimal maxAmount, BigDecimal price) {

//        return Single.zip(profileManager.loadMyProfile(), walletManager.getFreshBase58ReceivePubKey(), (p, pk) ->
//                SellOffer.builder()
//                        .sellerProfilePubKey(p.getPubKey())
//                        .sellerEscrowPubKey(pk)
//                        .arbitratorProfilePubKey(arbitrator.getPubKey())
//                        .currencyCode(currencyCode)
//                        .paymentMethod(paymentMethod)
//                        .minAmount(minAmount)
//                        .maxAmount(maxAmount)
//                        .price(price)
//                        .build()
//        ).flatMap(o -> sellOfferService.put(o.getSellerEscrowPubKey(), o)).subscribeOn(Schedulers.io());
        return null;
    }

    private Single<List<SellOffer>> singleOffers() {
        return sellOfferService.get()
                .retryWhen(errors ->
                        errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS))
                )
                .subscribeOn(Schedulers.io());
    }

    private Observable<List<SellOffer>> observableOffers() {
        return Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .flatMap(tick -> sellOfferService.get()
                        .retryWhen(errors ->
                                errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS))
                        ).toObservable())
                .subscribeOn(Schedulers.io());
    }

    public Completable deleteOffer(String sellerEscrowPubKey) {
        return sellOfferService.delete(sellerEscrowPubKey).subscribeOn(Schedulers.io());
    }

    public Observable<SellOffer> getSelectedOffer() {
        return selectedOffer;
    }

    public void setSelectedOffer(SellOffer selectedOffer) {
        this.selectedOfferSubject.onNext(selectedOffer);
    }

    public Observable<List<SellOffer>> getOffers() {
        return offers;
    }
}