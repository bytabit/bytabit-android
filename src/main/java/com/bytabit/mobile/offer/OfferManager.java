package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.offer.model.Offer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.schedulers.JavaFxScheduler;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(OfferManager.class);

    private final OffersService offersService;

    private final ObservableList<Offer> offersObservableList;

    public OfferManager() {
        super();
        offersService = retrofit.create(OffersService.class);
        offersObservableList = FXCollections.observableArrayList();
    }

    public void createOffer(String pubKey, String sellerPubKey, CurrencyCode currencyCode,
                            PaymentMethod paymentMethod, BigDecimal minAmount,
                            BigDecimal maxAmount, BigDecimal price) {

        Offer offer = null;
        try {
            offer = offersService.createOffer(new Offer(pubKey, sellerPubKey, currencyCode,
                    paymentMethod, minAmount, maxAmount, price)).execute().body();
            offersObservableList.add(offer);
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
    }

    public ObservableList<Offer> getOffersObservableList() {
        return offersObservableList;
    }

    public void startOfferPolling() {
        try {
            List<Offer> offers = offersService.readOffers().execute().body();
            offersObservableList.setAll(offers);
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
        Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> offersService.readOffers())
                .retry()
                .observeOn(JavaFxScheduler.getInstance())
                .subscribe(c -> {
                    try {
                        List<Offer> offers = c.execute().body();
                        offersObservableList.setAll(offers);
                    } catch (IOException ioe) {
                        LOG.error(ioe.getMessage());
                    }
                });
    }
}