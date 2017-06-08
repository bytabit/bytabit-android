package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import rx.Observable;
import rx.schedulers.JavaFxScheduler;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(OfferManager.class);

    private final SellOfferService sellOfferService;

    private final ObservableList<SellOffer> sellOffersObservableList;

    private final SellOffer newSellOffer;

    private final SellOffer viewSellOffer;

    private final ObjectProperty<BigDecimal> buyBtcAmount;

    public OfferManager() {
        Retrofit sellOfferRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(SellOffer.class))
                .build();

        sellOfferService = sellOfferRetrofit.create(SellOfferService.class);

        sellOffersObservableList = FXCollections.observableArrayList();
        newSellOffer = new SellOffer();
        viewSellOffer = new SellOffer();
        buyBtcAmount = new SimpleObjectProperty<>();
    }

    public void createOffer() {

        try {
            SellOffer createdOffer = sellOfferService.post(newSellOffer).execute().body();
            sellOffersObservableList.add(createdOffer);
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
    }

    public void readOffers() {
        try {
            List<SellOffer> offers = sellOfferService.get().execute().body();
            sellOffersObservableList.setAll(offers);
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
        Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> sellOfferService.get())
                .retry()
                .observeOn(JavaFxScheduler.getInstance())
                .subscribe(c -> {
                    try {
                        List<SellOffer> offers = c.execute().body();
                        sellOffersObservableList.setAll(offers);
                    } catch (IOException ioe) {
                        LOG.error(ioe.getMessage());
                    }
                });
    }

    public void deleteOffer() {

        try {
            sellOfferService.delete(viewSellOffer.getSellerEscrowPubKey()).execute().body();
            for (SellOffer so : sellOffersObservableList) {
                if (so.getSellerEscrowPubKey().equals(viewSellOffer.getSellerEscrowPubKey())) {
                    sellOffersObservableList.remove(so);
                }
            }
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
    }

    public ObservableList<SellOffer> getSellOffersObservableList() {
        return sellOffersObservableList;
    }

    public SellOffer newOffer() {
        return newSellOffer;
    }

    public SellOffer getViewSellOffer() {
        return viewSellOffer;
    }

    public ObjectProperty<BigDecimal> getBuyBtcAmount() {
        return buyBtcAmount;
    }
}