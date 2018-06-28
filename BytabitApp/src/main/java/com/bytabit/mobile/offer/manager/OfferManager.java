package com.bytabit.mobile.offer.manager;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.trade.manager.TradeManager;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferManager extends AbstractManager {

    private final EventLogger eventLogger = EventLogger.of(OfferManager.class);

    private final PublishSubject<SellOffer> selectedOffer = PublishSubject.create();

    private final SellOfferService sellOfferService;

    private Observable<List<SellOffer>> offers;

    @Inject
    ProfileManager profileManager;

    @Inject
    WalletManager walletManager;

    @Inject
    TradeManager tradeManager;

    public OfferManager() {
        sellOfferService = new SellOfferService();
    }

    @PostConstruct
    public void initialize() {

        offers = Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .flatMap(tick -> getLoadedOffers());
    }

    public Observable<List<SellOffer>> getLoadedOffers() {
        return sellOfferService.getAll().retryWhen(errors ->
                errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS)))
                .toObservable();
    }

    public Observable<List<SellOffer>> getUpdatedOffers() {
        return offers.share();
    }

    public Observable<SellOffer> createOffer(CurrencyCode currencyCode, PaymentMethod paymentMethod, String arbitratorProfilePubKey,
                                             BigDecimal minAmount, BigDecimal maxAmount, BigDecimal price) {

        return Observable.zip(profileManager.loadMyProfile(), walletManager.getTradeWalletEscrowPubKey(), (p, pk) ->
                SellOffer.builder()
                        .sellerProfilePubKey(p.getPubKey())
                        .sellerEscrowPubKey(pk)
                        .arbitratorProfilePubKey(arbitratorProfilePubKey)
                        .currencyCode(currencyCode)
                        .paymentMethod(paymentMethod)
                        .minAmount(minAmount)
                        .maxAmount(maxAmount)
                        .price(price)
                        .build()
        ).flatMap(o -> sellOfferService.put(o).toObservable());
    }

    public Observable<SellOffer> deleteOffer(String sellerEscrowPubKey) {
        return sellOfferService.delete(sellerEscrowPubKey).toObservable();
    }

    public void setSelectedOffer(SellOffer sellOffer) {
        selectedOffer.onNext(sellOffer);
    }

    public Observable<SellOffer> getSelectedOffer() {
        return selectedOffer
                .compose(eventLogger.logObjects("Selected"))
                .share();
    }

    public Observable<Trade> createBuyOffer(BigDecimal btcAmount) {
        return getSelectedOffer().lastOrError().toObservable()
                .flatMap(sellOffer -> tradeManager.createBuyOffer(sellOffer, btcAmount));
    }
}