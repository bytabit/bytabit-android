package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.*;
import com.bytabit.mobile.offer.model.SellOffer;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferManager extends AbstractManager {

    private final EventLogger eventLogger = EventLogger.of(OfferManager.class);

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
                .startWith(new LoadOffers())
                .share();

        Observable<OfferResult> offerUpdates = Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .flatMap(tick -> sellOfferService.getAll().retryWhen(errors ->
                        errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS)))
                        .toObservable()).map(OffersUpdated::new);

        Observable<OfferResult> offerUpdatedResults = actionObservable.ofType(CreateSellOffer.class).map(e ->
                sellOfferService.put(e.getOffer()))
                .flatMap(Single::toObservable)
                .map(OfferUpdated::new);

        Observable<OfferResult> loadOffersResults = actionObservable.ofType(LoadOffers.class)
                .flatMap(e -> sellOfferService.getAll().toObservable())
                .map(OffersUpdated::new);

        Observable<OfferResult> selectOfferResults = actionObservable.ofType(SelectOffer.class)
                .map(SelectOffer::getOffer)
                .map(OfferSelected::new);

        Observable<OfferResult> removeOfferResults = actionObservable.ofType(RemoveOffer.class)
                .flatMap(e -> sellOfferService.delete(e.sellerEscrowPubKey).toObservable())
                .map(SellOffer::getSellerEscrowPubKey)
                .map(OfferRemoved::new);

        results = Observable.merge(loadOffersResults, offerUpdates, offerUpdatedResults,
                selectOfferResults).mergeWith(removeOfferResults)
                .onErrorReturn(OfferError::new)
                .compose(eventLogger.logResults())
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
//        offers = sellOfferService.getAll().flattenAsObservable(o -> o);
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

//    public Single<List<SellOffer>> createOffer(CurrencyCode currencyCode, PaymentMethod paymentMethod, Profile arbitrator,
//                                         BigDecimal minAmount, BigDecimal maxAmount, BigDecimal price) {

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
//        return null;
//    }

//    private Single<List<SellOffer>> singleOffers() {
//        return sellOfferService.getAll()
//                .retryWhen(errors ->
//                        errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS))
//                )
//                .subscribeOn(Schedulers.io());
//    }

//    private Observable<List<SellOffer>> observableOffers() {
//        return Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
//                .flatMap(tick -> sellOfferService.getAll()
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

//    public Observable<List<SellOffer>> getAll() {
//        return offers;
//    }

    // Offer Action classes

    public interface OfferAction extends Event {
    }

    public class LoadOffers implements OfferAction {
    }

    public class CreateSellOffer implements OfferAction {
        final private SellOffer offer;

        public CreateSellOffer(SellOffer offer) {
            this.offer = offer;
        }

        public SellOffer getOffer() {
            return offer;
        }
    }

    public class SelectOffer implements OfferAction {
        private final SellOffer offer;

        public SelectOffer(SellOffer offer) {
            this.offer = offer;
        }

        public SellOffer getOffer() {
            return offer;
        }
    }

    public class RemoveOffer implements OfferAction {
        final private String sellerEscrowPubKey;

        public RemoveOffer(String sellerEscrowPubKey) {
            this.sellerEscrowPubKey = sellerEscrowPubKey;
        }

        public String getSellerEscrowPubKey() {
            return sellerEscrowPubKey;
        }
    }

    // Offer Result classes

    public interface OfferResult extends Result {
    }

    public class OfferUpdated implements OfferResult {

        private final SellOffer offer;

        public OfferUpdated(SellOffer offer) {
            this.offer = offer;
        }

        public SellOffer getOffer() {
            return offer;
        }
    }

    public class OffersUpdated implements OfferResult {

        private final List<SellOffer> offers;

        public OffersUpdated(List<SellOffer> offers) {
            this.offers = offers;
        }

        public List<SellOffer> getOffers() {
            return offers;
        }
    }

    public class OfferSelected implements OfferResult {
        private final SellOffer offer;

        public OfferSelected(SellOffer offer) {
            this.offer = offer;
        }

        public SellOffer getOffer() {
            return offer;
        }
    }

    public class OfferRemoved implements OfferResult {
        final private String sellerEscrowPubKey;

        public OfferRemoved(String sellerEscrowPubKey) {
            this.sellerEscrowPubKey = sellerEscrowPubKey;
        }

        public String getSellerEscrowPubKey() {
            return sellerEscrowPubKey;
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