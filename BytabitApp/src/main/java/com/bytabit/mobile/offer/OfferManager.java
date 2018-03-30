package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.*;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class OfferManager extends AbstractManager {

    private final EventLogger eventLogger = EventLogger.of(ProfileManager.class);

    private final SellOfferService sellOfferService;

    private final PublishSubject<OfferAction> actions;

    private final Observable<OfferResult> results;

//    @Inject
//    ProfileManager profileManager;
//
//    @Inject
//    WalletManager walletManager;

//    private final Observable<SellOffer> offers;

//    private final PublishSubject<SellOffer> selectedOfferSubject;
//
//    private final Observable<SellOffer> selectedOffer;

    public OfferManager() {

        sellOfferService = new SellOfferService();

        actions = PublishSubject.create();

        Observable<OfferAction> actionObservable = actions
                .compose(eventLogger.logEvents())
                .share();

        Observable<OfferResult> offerUpdates = Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .flatMap(tick -> sellOfferService.get().retryWhen(errors ->
                        errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS)))
                        .flattenAsObservable(o -> o)).map(OfferUpdated::new);

        Observable<OfferResult> offerUpdatedResults = actionObservable.ofType(CreateSellOffer.class).map(e ->
                sellOfferService.put(e.getOffer()))
                .flatMap(Single::toObservable)
                .map(OfferUpdated::new);

        results = Observable.merge(offerUpdates, offerUpdatedResults)
                .onErrorReturn(OfferError::new)
                .share();

//        offers = Observable.concat(singleOffers().toObservable(), observableOffers())
//                .subscribeOn(Schedulers.io()).publish().autoConnect();

//        Single<SellOffer> putSellOffer = sellOfferService.put(SellOffer.builder()
//                .sellerEscrowPubKey("TEST20180316-2")
//                .build());
//
//        Single<SellOffer> deleteSellOffer = sellOfferService.delete("TEST20180316-2");
//
//        SellOffer sellOfferPut = putSellOffer.blockingGet();
//
//        deleteSellOffer.subscribe(so -> LOG.info("Deleted Offer " + so.getSellerEscrowPubKey()));
//        offers = sellOfferService.get().flattenAsObservable(o -> o);
//
//        offers.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).forEach(o ->
//                LOG.info(String.format("Found offer: %s", o.getSellerEscrowPubKey()))
//        );
//        offers.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).doOnError(o ->
//                LOG.error(o.toString())
//        );

//        selectedOfferSubject = PublishSubject.create();
//        selectedOffer = selectedOfferSubject.publish().autoConnect()
//                .subscribeOn(Schedulers.io());
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
//        return sellOfferService.get()
//                .retryWhen(errors ->
//                        errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS))
//                )
//                .subscribeOn(Schedulers.io());
//    }

//    private Observable<List<SellOffer>> observableOffers() {
//        return Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
//                .flatMap(tick -> sellOfferService.get()
//                        .retryWhen(errors ->
//                                errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS))
//                        ).toObservable())
//                .subscribeOn(Schedulers.io());
//    }

    public Single<SellOffer> deleteOffer(String sellerEscrowPubKey) {
        return sellOfferService.delete(sellerEscrowPubKey).subscribeOn(Schedulers.io());
    }

    public PublishSubject<OfferAction> getActions() {
        return actions;
    }

    public Observable<OfferResult> getResults() {
        return results;
    }

    //    public Observable<SellOffer> getSelectedOffer() {
//        return selectedOffer;
//    }
//
//    public void setSelectedOffer(SellOffer selectedOffer) {
//        this.selectedOfferSubject.onNext(selectedOffer);
//    }

//    public Observable<List<SellOffer>> get() {
//        return offers;
//    }

    // Offer Action classes

    public interface OfferAction extends Event {
    }

    public class CreateSellOffer implements OfferAction {
        private SellOffer offer;

        public CreateSellOffer(SellOffer offer) {
            this.offer = offer;
        }

        public SellOffer getOffer() {
            return offer;
        }
    }

    // Offer Result classes

    public interface OfferResult extends Result {
    }

    public class OfferUpdated implements OfferResult {

        private SellOffer offer;

        public OfferUpdated(SellOffer offer) {
            this.offer = offer;
        }

        public SellOffer getOffer() {
            return offer;
        }
    }

    public class OfferError implements OfferResult, ErrorResult {
        private final Throwable error;

        public OfferError(Throwable error) {
            this.error = error;
        }

        @Override
        public Throwable getError() {
            return error;
        }
    }
}