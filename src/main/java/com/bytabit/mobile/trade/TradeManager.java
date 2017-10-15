package com.bytabit.mobile.trade;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.model.Trade;
import com.fasterxml.jackson.jr.ob.JSON;
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

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bytabit.mobile.trade.model.Trade.Role.*;
import static com.bytabit.mobile.trade.model.Trade.Status.CREATED;

public class TradeManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(TradeManager.class);

    @Inject
    private BuyerProtocol buyerProtocol;

    @Inject
    private SellerProtocol sellerProtocol;

    @Inject
    private ArbitratorProtocol arbitratorProtocol;

    @Inject
    private ProfileManager profileManager;

    private final TradeService tradeService;

    private final ObservableList<Trade> tradesObservableList;

    private final ObjectProperty<Trade> selectedTrade;

    public final static String TRADES_PATH = AppConfig.getPrivateStorage().getPath() + File.separator +
            "trades" + File.separator;

    public TradeManager() {

        Retrofit tradeRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(Trade.class))
                .build();
        tradeService = tradeRetrofit.create(TradeService.class);

        tradesObservableList = FXCollections.observableArrayList();
        selectedTrade = new SimpleObjectProperty<>();

        readTrades();
        updateTrades();
    }

    public ObservableList<Trade> getTradesObservableList() {
        return tradesObservableList;
    }

    public ObjectProperty<Trade> getSelectedTradeProperty() {
        return selectedTrade;
    }

    public Trade buyerCreateTrade(SellOffer sellOffer, BigDecimal buyBtcAmount,
                                  String buyerEscrowPubKey, String buyerProfilePubKey,
                                  String buyerPayoutAddress) {

        return buyerProtocol.createTrade(sellOffer, buyBtcAmount, buyerEscrowPubKey,
                buyerProfilePubKey, buyerPayoutAddress);
    }

    public void buyerSendPayment(String paymentReference) {
        buyerProtocol.sendPayment(selectedTrade.getValue(), paymentReference);
    }

    public void sellerConfirmPaymentReceived() {
        sellerProtocol.confirmPaymentReceived(selectedTrade.getValue());
    }

    public void requestArbitrate() {
        Trade trade = selectedTrade.getValue();

        String profilePubKey = profileManager.getPubKeyProperty().getValue();
        Boolean profileIsArbitrator = profileManager.getIsArbitratorProperty().getValue();

        Trade.Role role = trade.getRole(profilePubKey, profileIsArbitrator);
        if (role.equals(SELLER)) {
            sellerProtocol.requestArbitrate(trade);
        } else if (role.equals(BUYER)) {
            buyerProtocol.requestArbitrate(trade);
        }
    }

    public void arbitratorRefundSeller() {
        Trade trade = selectedTrade.getValue();
        arbitratorProtocol.refundSeller(trade);
    }

    public void arbitratorPayoutBuyer() {
        Trade trade = selectedTrade.getValue();
        arbitratorProtocol.payoutBuyer(trade);
    }

    public void buyerCancel() {
        Trade trade = selectedTrade.getValue();
        buyerProtocol.cancelTrade(trade);
    }

    public boolean activeSellerEscrowPubKey(String sellerEscrowPubKey) {
        for (Trade trade : tradesObservableList) {
            if (trade.getSellerEscrowPubKey().equals(sellerEscrowPubKey) && trade.getStatus().equals(CREATED)) {
                return true;
            }
        }
        return false;
    }

    public boolean activeBuyerEscrowPubKey(String buyerEscrowPubKey) {
        for (Trade trade : tradesObservableList) {
            if (trade.getBuyerEscrowPubKey().equals(buyerEscrowPubKey) && trade.getStatus().equals(CREATED)) {
                return true;
            }
        }
        return false;
    }

    public void setSelectedTrade(Trade trade) {
        selectedTrade.setValue(trade);
    }

    private void readTrades() {

        // load stored trades
        File tradesDir = new File(TRADES_PATH);
        if (tradesDir.list() != null) {
            for (String tradeId : tradesDir.list()) {

                try {
                    File tradeFile = new File(TRADES_PATH + tradeId + File.separator + "trade.json");
                    FileReader tradeReader = new FileReader(tradeFile);
                    Trade trade = JSON.std.beanFrom(Trade.class, tradeReader);

                    tradesObservableList.add(trade);
                } catch (IOException ioe) {
                    LOG.error(ioe.getMessage());
                }
            }
        } else {
            tradesDir.mkdirs();
        }
    }

    private void updateTrades() {

        Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> tradeService.get(profileManager.getPubKeyProperty().getValue()))
                .retry()
                .observeOn(JavaFxScheduler.getInstance())
                .subscribe(c -> {
                    LOG.debug(c.toString());
                    try {
                        List<Trade> trades = c.execute().body();
                        receiveTrades(trades);
                    } catch (IOException ioe) {
                        LOG.error(ioe.getMessage());
                    }
                });
    }

    private void receiveTrades(List<Trade> trades) {
        for (Trade trade : trades) {
            Trade updatedTrade = handleTrade(trade);
            if (updatedTrade != null) {
                writeUpdatedTrade(trade);
                updateTradesList(trade);
            }
        }
    }

    private Trade handleTrade(Trade trade) {

        Trade currentTrade = getTrade(trade.getEscrowAddress());
        Trade updatedTrade = null;

        if (currentTrade == null || !currentTrade.getStatus().equals(trade.getStatus())) {

            String profilePubKey = profileManager.getPubKeyProperty().getValue();
            Boolean profileIsArbitrator = profileManager.getIsArbitratorProperty().getValue();

            Trade.Role role = trade.getRole(profilePubKey, profileIsArbitrator);
            TradeProtocol tradeProtocol;

            if (role.equals(SELLER)) {
                tradeProtocol = sellerProtocol;
            } else if (role.equals(BUYER)) {
                tradeProtocol = buyerProtocol;
            } else if (role.equals(ARBITRATOR)) {
                tradeProtocol = arbitratorProtocol;
            } else {
                throw new RuntimeException("Unable to determine trade protocol.");
            }

            switch (trade.getStatus()) {

                case CREATED:
                    updatedTrade = currentTrade == null ? tradeProtocol.handleCreated(trade) : null;
                    break;

                case FUNDED:
                    updatedTrade = tradeProtocol.handleFunded(currentTrade, trade);
                    break;

                case PAID:
                    updatedTrade = tradeProtocol.handlePaid(currentTrade, trade);
                    break;

                case COMPLETED:
                    updatedTrade = tradeProtocol.handleCompleted(currentTrade, trade);
                    break;

                case ARBITRATING:
                    updatedTrade = tradeProtocol.handleArbitrating(currentTrade, trade);
                    break;
            }
        }

        return updatedTrade;
    }

    private Trade getTrade(String escrowAddress) {
        Trade foundTrade = null;

        for (Trade trade : tradesObservableList) {
            if (trade.getEscrowAddress().equals(escrowAddress)) {
                foundTrade = trade;
                break;
            }
        }

        return foundTrade;
    }

    private void writeUpdatedTrade(Trade updatedTrade) {

        String tradePath = TRADES_PATH + updatedTrade.getEscrowAddress() + File.separator + "trade.json";

        try {
            FileWriter tradeWriter = new FileWriter(tradePath);
            tradeWriter.write(JSON.std.asString(updatedTrade));
            tradeWriter.flush();

            LOG.debug("Created local trade: {}", updatedTrade);

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    private void updateTradesList(Trade updatedTrade) {
        int index = tradesObservableList.indexOf(updatedTrade);
        tradesObservableList.set(index, updatedTrade);
    }
}