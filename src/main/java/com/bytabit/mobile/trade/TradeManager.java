package com.bytabit.mobile.trade;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.BuyRequest;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.WalletManager;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TradeManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(TradeManager.class);

    private final BuyRequestService buyRequestService;

    private final ObservableList<Trade> tradesObservableList;

    private final Trade viewTrade;

    String tradesPath = AppConfig.getPrivateStorage().getPath() + File.separator +
            "trades" + File.separator;

    public TradeManager() {
        Retrofit buyRequestRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(BuyRequest.class))
                .build();

        buyRequestService = buyRequestRetrofit.create(BuyRequestService.class);
        tradesObservableList = FXCollections.observableArrayList();
        viewTrade = new Trade();
    }

    public String createBuyRequest(SellOffer sellOffer, BigDecimal buyBtcAmount,
                                   NetworkParameters params, String buyerEscrowPubKey,
                                   String buyerProfilePubKey, String buyerPayoutAddress) {

        try {
            BuyRequest newBuyRequest = new BuyRequest(sellOffer.getSellerEscrowPubKey(),
                    buyerEscrowPubKey, buyBtcAmount, buyerProfilePubKey, buyerPayoutAddress);

            String apk = sellOffer.arbitratorProfilePubKeyProperty().get();
            String spk = sellOffer.sellerEscrowPubKeyProperty().get();
            BuyRequest createdBuyRequest = buyRequestService.createBuyRequest(spk, newBuyRequest).execute().body();

            String tradeEscrowAddress = WalletManager.escrowAddress(params, apk, spk, buyerProfilePubKey);

            String tradePath = tradesPath + tradeEscrowAddress;
            File tradeDir = new File(tradePath);
            tradeDir.mkdirs();

            String sellOfferPath = tradePath + File.separator + "sellOffer.json";
            FileWriter sellOfferFileWriter = new FileWriter(sellOfferPath);
            sellOfferFileWriter.write(JSON.std.asString(sellOffer));
            sellOfferFileWriter.flush();

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

    public ObservableList<Trade> getTradesObservableList() {
        return tradesObservableList;
    }

    public Trade getViewTrade() {
        return viewTrade;
    }

    public void readTrades() {
        File tradesDir = new File(tradesPath);
        List<String> tradePaths;
        if (tradesDir.list() != null) {
            tradePaths = Arrays.asList(tradesDir.list());
        } else {
            tradePaths = new ArrayList<>();
        }
    }
}