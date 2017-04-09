package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.BuyRequest;
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

    private final OfferService offerService;

    private final ObservableList<SellOffer> offersObservableList;

    private final SellOffer newOffer;

    private final SellOffer viewOffer;

    private final ObjectProperty<BigDecimal> buyBtcAmount;

    public OfferManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(SellOffer.class))
                .addConverterFactory(new JacksonJrConverter<>(BuyRequest.class))
                .build();

        offerService = retrofit.create(OfferService.class);
        offersObservableList = FXCollections.observableArrayList();
        newOffer = new SellOffer();
        viewOffer = new SellOffer();
        buyBtcAmount = new SimpleObjectProperty<>();
    }

    public void createOffer() {

        try {
            SellOffer createdOffer = offerService.createOffer(newOffer).execute().body();
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

    public void createBuyRequest(String buyerEscrowPubKey, String buyerProfilePubKey, String buyerPayoutAddress) {

        try {
            BuyRequest newBuyRequest = new BuyRequest(viewOffer.getSellerEscrowPubKey(),
                    buyerEscrowPubKey, buyBtcAmount.get(), buyerProfilePubKey, buyerPayoutAddress);

            BuyRequest createdBuyRequest = offerService.createBuyRequest(newBuyRequest).execute().body();
            LOG.debug("Created buy request: %s", createdBuyRequest.toString());
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
    }

    public ObservableList<SellOffer> getOffersObservableList() {
        return offersObservableList;
    }

    public SellOffer newOffer() {
        return newOffer;
    }

    public SellOffer getViewOffer() {
        return viewOffer;
    }

    public ObjectProperty<BigDecimal> getBuyBtcAmount() {
        return buyBtcAmount;
    }
}