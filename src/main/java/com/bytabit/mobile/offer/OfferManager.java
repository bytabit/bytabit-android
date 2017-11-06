package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.wallet.WalletManager;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

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

    private final ObjectProperty<SellOffer> selectedOffer = new SimpleObjectProperty<>();
    private final ObservableList<SellOffer> offers = FXCollections.observableArrayList();

    public OfferManager() {
        Retrofit sellOfferRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(new JacksonJrConverter<>(SellOffer.class))
                .build();

        sellOfferService = sellOfferRetrofit.create(SellOfferService.class);
    }

    public Single<SellOffer> createOffer(CurrencyCode currencyCode, PaymentMethod paymentMethod, Profile arbitrator,
                                         BigDecimal minAmount, BigDecimal maxAmount, BigDecimal price) {

        return Single.zip(profileManager.retrieveMyProfile(), walletManager.getFreshBase58ReceivePubKey(), (p, pk) ->
                SellOffer.builder()
                        .sellerProfilePubKey(p.getPubKey())
                        .sellerEscrowPubKey(pk)
                        .arbitratorProfilePubKey(arbitrator.getPubKey())
                        .currencyCode(currencyCode)
                        .paymentMethod(paymentMethod)
                        .minAmount(minAmount)
                        .maxAmount(maxAmount)
                        .price(price)
                        .build()
        ).flatMap(o -> sellOfferService.put(o.getSellerEscrowPubKey(), o)).subscribeOn(Schedulers.io());
    }

    public Single<List<SellOffer>> singleOffers() {
        return sellOfferService.get().retry().subscribeOn(Schedulers.io());
    }

    public Observable<List<SellOffer>> observableOffers() {
        return Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .flatMap(tick -> sellOfferService.get().retry().toObservable())
                .subscribeOn(Schedulers.io());
    }

    public Completable deleteOffer(String sellerEscrowPubKey) {
        return sellOfferService.delete(sellerEscrowPubKey).subscribeOn(Schedulers.io());
    }

    public SellOffer getSelectedOffer() {
        return selectedOffer.get();
    }

    public void setSelectedOffer(SellOffer selectedOffer) {
        this.selectedOffer.set(selectedOffer);
    }

    public ObjectProperty<SellOffer> selectedOfferProperty() {
        return selectedOffer;
    }

    public ObservableList<SellOffer> getOffers() {
        return offers;
    }
}