package com.bytabit.mobile.trade;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.OfferService;
import com.bytabit.mobile.offer.model.SellOffer;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import rx.Observable;
import rx.schedulers.JavaFxScheduler;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TradeManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(TradeManager.class);

    private final OfferService offerService;

    private final ObservableList<SellOffer> offersObservableList;

    private final SellOffer newOffer;

    private final SellOffer viewOffer;

    public TradeManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(SellOffer.class))
                .build();
        
        offerService = retrofit.create(OfferService.class);
        offersObservableList = FXCollections.observableArrayList();
        newOffer = new SellOffer();
        viewOffer = new SellOffer();
    }

    public void createOffer() {

        try {
            SellOffer createdOffer = offerService.create(newOffer).execute().body();
            offersObservableList.add(createdOffer);
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
    }

    public void readOffers() {
        try {
            List<SellOffer> offers = offerService.read().execute().body();
            offersObservableList.setAll(offers);
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
        Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> offerService.read())
                .retry()
                .observeOn(JavaFxScheduler.getInstance())
                .subscribe(c -> {
                    try {
                        List<SellOffer> offers = c.execute().body();
                        offersObservableList.setAll(offers);
                    } catch (IOException ioe) {
                        LOG.error(ioe.getMessage());
                    }
                });
    }

    public void deleteOffer() {

        try {
            SellOffer removedOffer = offerService.delete(viewOffer.getSellerEscrowPubKey()).execute().body();
            if (removedOffer != null) {
                offersObservableList.removeIf(o -> o.getSellerEscrowPubKey().equals(removedOffer.getSellerEscrowPubKey()));
            }
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
    }

    public ObservableList<SellOffer> getOffersObservableList() {
        return offersObservableList;
    }

    public SellOffer getNewOffer() {
        return newOffer;
    }

    public SellOffer getViewOffer() {
        return viewOffer;
    }
}