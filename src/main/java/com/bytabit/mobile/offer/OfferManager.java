package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.offer.model.Offer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.schedulers.JavaFxScheduler;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(OfferManager.class);

    private final OffersService offersService;

    private final ObservableList<Offer> offersObservableList;

    private final Offer newOffer;

    private final Offer viewOffer;

    public OfferManager() {
        super();
        offersService = retrofit.create(OffersService.class);
        offersObservableList = FXCollections.observableArrayList();
        newOffer = new Offer();
        viewOffer = new Offer();
    }

    public void createOffer() {

        try {
            Offer createdOffer = offersService.createOffer(newOffer).execute().body();
            offersObservableList.add(createdOffer);
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
    }

    public void readOffers() {
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

    public ObservableList<Offer> getOffersObservableList() {
        return offersObservableList;
    }

    public Offer getNewOffer() {
        return newOffer;
    }

    public Offer getViewOffer() {
        return viewOffer;
    }
}