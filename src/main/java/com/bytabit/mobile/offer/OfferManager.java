package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.wallet.WalletManager;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import rx.Observable;
import rx.schedulers.JavaFxScheduler;
import rx.schedulers.Schedulers;

import javax.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(OfferManager.class);

    @Inject
    private WalletManager walletManager;

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
                Platform.runLater(() -> {
                    sellOffersObservableList.add(createdOffer);
                });
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
            Platform.runLater(() -> {
                sellOffersObservableList.setAll(offers);
            });
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
                        Platform.runLater(() -> {
                            sellOffersObservableList.setAll(offers);
                        });
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
                    Platform.runLater(() -> {
                        sellOffersObservableList.remove(so);
                    });
                }
            }
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
    }

    public StringProperty getSellerEscrowPubKeyProperty() {
        return sellerEscrowPubKeyProperty;
    }

    public StringProperty getSellerProfilePubKeyProperty() {
        return sellerProfilePubKeyProperty;
    }

    public StringProperty getArbitratorProfilePubKeyProperty() {
        return arbitratorProfilePubKeyProperty;
    }

    public ObjectProperty<CurrencyCode> getCurrencyCodeProperty() {
        return currencyCodeProperty;
    }

    public ObjectProperty<PaymentMethod> getPaymentMethodProperty() {
        return paymentMethodProperty;
    }

    public ObjectProperty<BigDecimal> getMinAmountProperty() {
        return minAmountProperty;
    }

    public ObjectProperty<BigDecimal> getMaxAmountProperty() {
        return maxAmountProperty;
    }

    public ObjectProperty<BigDecimal> getPriceProperty() {
        return priceProperty;
    }

    public ObjectProperty<BigDecimal> getBuyBtcAmountProperty() {
        return buyBtcAmountProperty;
    }

    public ObjectProperty<SellOffer> getSelectedSellOfferProperty() {
        return selectedSellOfferProperty;
    }

    public ObservableList<SellOffer> getSellOffersObservableList() {
        return sellOffersObservableList;
    }
}