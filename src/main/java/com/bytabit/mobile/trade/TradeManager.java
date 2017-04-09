package com.bytabit.mobile.trade;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.BuyRequest;
import com.bytabit.mobile.trade.model.PaymentRequest;
import com.bytabit.mobile.trade.model.PayoutRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;

public class TradeManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(TradeManager.class);

    private final TradeService tradeService;

    private final ObservableList<Trade> tradesObservableList;

    private final Trade activeTrade;

    private final Trade viewTrade;

    public TradeManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(BuyRequest.class))
                .addConverterFactory(new JacksonJrConverter<>(PaymentRequest.class))
                .addConverterFactory(new JacksonJrConverter<>(PayoutRequest.class))
                .addConverterFactory(new JacksonJrConverter<>(Trade.class))
                .build();

        tradeService = retrofit.create(TradeService.class);
        tradesObservableList = FXCollections.observableArrayList();
        activeTrade = new Trade();
        viewTrade = new Trade();
    }

//    public void readOffers() {
//        try {
//            List<SellOffer> offers = offerService.read().execute().body();
//            offersObservableList.setAll(offers);
//        } catch (IOException ioe) {
//            LOG.error(ioe.getMessage());
//        }
//        Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
//                .map(tick -> offerService.read())
//                .retry()
//                .observeOn(JavaFxScheduler.getInstance())
//                .subscribe(c -> {
//                    try {
//                        List<SellOffer> offers = c.execute().body();
//                        offersObservableList.setAll(offers);
//                    } catch (IOException ioe) {
//                        LOG.error(ioe.getMessage());
//                    }
//                });
//    }
//
//    public void deleteOffer() {
//
//        try {
//            SellOffer removedOffer = offerService.delete(viewOffer.getSellerEscrowPubKey()).execute().body();
//            if (removedOffer != null) {
//                offersObservableList.removeIf(o -> o.getSellerEscrowPubKey().equals(removedOffer.getSellerEscrowPubKey()));
//            }
//        } catch (IOException ioe) {
//            LOG.error(ioe.getMessage());
//        }
//    }
//
//    public ObservableList<SellOffer> getOffersObservableList() {
//        return offersObservableList;
//    }
//
//    public SellOffer getNewOffer() {
//        return newOffer;
//    }
//
//    public SellOffer getViewOffer() {
//        return viewOffer;
//    }
}