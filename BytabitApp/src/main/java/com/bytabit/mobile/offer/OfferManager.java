package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.wallet.manager.WalletManager;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;

public class OfferManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(OfferManager.class);

    private final SellOfferService sellOfferService;

    @Inject
    ProfileManager profileManager;

    @Inject
    WalletManager walletManager;

    private final Observable<SellOffer> offers;

    private final PublishSubject<SellOffer> selectedOfferSubject;

    private final Observable<SellOffer> selectedOffer;

    public OfferManager() {

        sellOfferService = new SellOfferService();

//        offers = Observable.concat(singleOffers().toObservable(), observableOffers())
//                .subscribeOn(Schedulers.io()).publish().autoConnect();

        Single<SellOffer> putSellOffer = sellOfferService.putOffer(SellOffer.builder()
                .sellerEscrowPubKey("TEST20180316-2")
                .build());

        Single<SellOffer> deleteSellOffer = sellOfferService.deleteOffer("TEST20180316-2");

        SellOffer sellOfferPut = putSellOffer.blockingGet();

        deleteSellOffer.subscribe(so -> LOG.info("Deleted Offer " + so.getSellerEscrowPubKey()));
        offers = sellOfferService.getOffers().flattenAsObservable(o -> o);

        offers.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).forEach(o ->
                LOG.info(String.format("Found offer: %s", o.getSellerEscrowPubKey()))
        );
        offers.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).doOnError(o ->
                LOG.error(o.toString())
        );

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

//    private Single<List<SellOffer>> singleOffers() {
//        return sellOfferService.getOffers()
//                .retryWhen(errors ->
//                        errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS))
//                )
//                .subscribeOn(Schedulers.io());
//    }

//    private Observable<List<SellOffer>> observableOffers() {
//        return Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
//                .flatMap(tick -> sellOfferService.getOffers()
//                        .retryWhen(errors ->
//                                errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS))
//                        ).toObservable())
//                .subscribeOn(Schedulers.io());
//    }

    public Single<SellOffer> deleteOffer(String sellerEscrowPubKey) {
        return sellOfferService.deleteOffer(sellerEscrowPubKey).subscribeOn(Schedulers.io());
    }

    public Observable<SellOffer> getSelectedOffer() {
        return selectedOffer;
    }

    public void setSelectedOffer(SellOffer selectedOffer) {
        this.selectedOfferSubject.onNext(selectedOffer);
    }

//    public Observable<List<SellOffer>> getOffers() {
//        return offers;
//    }
}