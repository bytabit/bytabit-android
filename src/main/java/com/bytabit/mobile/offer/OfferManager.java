package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
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

@Getter
public class OfferManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(OfferManager.class);

    private final SellOfferService sellOfferService;

    private final ObservableList<SellOffer> sellOffersObservableList;

    private final StringProperty sellerEscrowPubKeyProperty;
    private final StringProperty sellerProfilePubKeyProperty;
    private final StringProperty arbitratorProfilePubKeyProperty;

    private final ObjectProperty<CurrencyCode> currencyCodeProperty;
    private final ObjectProperty<PaymentMethod> paymentMethodProperty;
    private final ObjectProperty<BigDecimal> minAmountProperty;
    private final ObjectProperty<BigDecimal> maxAmountProperty;
    private final ObjectProperty<BigDecimal> priceProperty;

    private final ObjectProperty<BigDecimal> buyBtcAmountProperty;

    private final ObjectProperty<SellOffer> selectedSellOfferProperty;

    public OfferManager() {
        Retrofit sellOfferRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(SellOffer.class))
                .build();

        sellOfferService = sellOfferRetrofit.create(SellOfferService.class);

        sellOffersObservableList = FXCollections.observableArrayList();

        sellerEscrowPubKeyProperty = new SimpleStringProperty();
        sellerProfilePubKeyProperty = new SimpleStringProperty();
        arbitratorProfilePubKeyProperty = new SimpleStringProperty();

        currencyCodeProperty = new SimpleObjectProperty<>();
        paymentMethodProperty = new SimpleObjectProperty<>();
        minAmountProperty = new SimpleObjectProperty<>();
        maxAmountProperty = new SimpleObjectProperty<>();
        priceProperty = new SimpleObjectProperty<>();

        buyBtcAmountProperty = new SimpleObjectProperty<>();

        selectedSellOfferProperty = new SimpleObjectProperty<>();

        readOffers();
    }

    void createOffer() {

        try {
            SellOffer newSellOffer = SellOffer.builder()
                    .sellerEscrowPubKey(sellerEscrowPubKeyProperty.getValue())
                    .sellerProfilePubKey(sellerProfilePubKeyProperty.getValue())
                    .arbitratorProfilePubKey(arbitratorProfilePubKeyProperty.getValue())
                    .currencyCode(currencyCodeProperty.getValue())
                    .paymentMethod(paymentMethodProperty.getValue())
                    .minAmount(minAmountProperty.getValue())
                    .maxAmount(maxAmountProperty.getValue())
                    .price(priceProperty.getValue())
                    .build();
            if (newSellOffer.isComplete()) {
                SellOffer createdOffer = sellOfferService.put(newSellOffer.getSellerEscrowPubKey(), newSellOffer).execute().body();
                sellOffersObservableList.add(createdOffer);
            } else {
                LOG.error("Sell offer is incomplete.");
            }
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
    }

    void readOffers() {
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

    void deleteOffer() {

        try {
            sellOfferService.delete(sellerEscrowPubKeyProperty.getValue()).execute();
            for (SellOffer so : sellOffersObservableList) {
                if (so.getSellerEscrowPubKey().equals(sellerEscrowPubKeyProperty.getValue())) {
                    sellOffersObservableList.remove(so);
                }
            }
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
    }
}