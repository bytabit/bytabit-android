package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.BuyRequest;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.wallet.WalletManager;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import rx.Observable;
import rx.schedulers.JavaFxScheduler;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(OfferManager.class);

    private final SellOfferService sellOfferService;

    private final BuyRequestService buyRequestService;

    private final ObservableList<SellOffer> sellOffersObservableList;

    private final SellOffer newSellOffer;

    private final SellOffer viewSellOffer;

    private final ObjectProperty<BigDecimal> buyBtcAmount;

    public OfferManager() {
        Retrofit retrofitSellOffer = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(SellOffer.class))
                .build();

        sellOfferService = retrofitSellOffer.create(SellOfferService.class);

        Retrofit retrofitBuyRequest = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(BuyRequest.class))
                .build();

        buyRequestService = retrofitBuyRequest.create(BuyRequestService.class);

        sellOffersObservableList = FXCollections.observableArrayList();
        newSellOffer = new SellOffer();
        viewSellOffer = new SellOffer();
        buyBtcAmount = new SimpleObjectProperty<>();
    }

    public void createOffer() {

        try {
            SellOffer createdOffer = sellOfferService.createOffer(newSellOffer).execute().body();
            sellOffersObservableList.add(createdOffer);
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
    }

    public void readOffers() {
        try {
            List<SellOffer> offers = sellOfferService.read().execute().body();
            sellOffersObservableList.setAll(offers);
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }
        Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> sellOfferService.read())
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

    public String createBuyRequest(NetworkParameters params, String buyerEscrowPubKey, String buyerProfilePubKey, String buyerPayoutAddress) {

        try {
            BuyRequest newBuyRequest = new BuyRequest(viewSellOffer.getSellerEscrowPubKey(),
                    buyerEscrowPubKey, buyBtcAmount.get(), buyerProfilePubKey, buyerPayoutAddress);

            String apk = viewSellOffer.arbitratorProfilePubKeyProperty().get();
            String spk = viewSellOffer.sellerEscrowPubKeyProperty().get();
            BuyRequest createdBuyRequest = buyRequestService.createBuyRequest(spk, newBuyRequest).execute().body();

            String tradeEscrowAddress = WalletManager.escrowAddress(params, apk, spk, buyerProfilePubKey);

            String tradePath = AppConfig.getPrivateStorage().getPath() + File.separator + "trades" + File.pathSeparator + tradeEscrowAddress;
            File tradeDir = new File(tradePath);
            tradeDir.mkdirs();
            String buyRequestPath = tradePath + File.separator + "buyRequest.json";
            FileWriter buyRequestFileWriter = new FileWriter(buyRequestPath);
            buyRequestFileWriter.write(JSON.std.asString(createdBuyRequest));
            buyRequestFileWriter.flush();
            LOG.debug("Created buy request: {}", createdBuyRequest.toString());
            return tradeEscrowAddress;
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
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