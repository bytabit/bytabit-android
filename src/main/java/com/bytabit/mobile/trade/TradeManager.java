package com.bytabit.mobile.trade;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.trade.model.Trade;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    private final ObservableList<Trade> trades = FXCollections.observableArrayList();

    private final ObjectProperty<Trade> selectedTrade = new SimpleObjectProperty<>();

    public final static String TRADES_PATH = AppConfig.getPrivateStorage().getPath() + File.separator +
            "trades" + File.separator;

    public TradeManager() {

        Retrofit tradeRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(new JacksonJrConverter<>(Trade.class))
                .build();
        tradeService = tradeRetrofit.create(TradeService.class);
    }

    public void initialize() {

//        singleStoredTrades().subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform())
//                .subscribe((Consumer<List<Trade>>) trades::addAll);

        singleStoredTrades().observeOn(Schedulers.io()).flattenAsObservable(tl -> tl).concatWith(
                watchUpdatedTrades().observeOn(Schedulers.io()).flatMap(trade -> writeTrade(trade).toObservable()))
                .subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform())
                .subscribe(updatedTrade -> {
                    int index = trades.indexOf(updatedTrade);
                    if (index > -1) {
                        trades.remove(index);
                    }
                    trades.add(updatedTrade);
                });
    }

    public Single<Trade> buyerCreateTrade(SellOffer sellOffer, BigDecimal buyBtcAmount,
                                          String buyerEscrowPubKey, String buyerProfilePubKey,
                                          String buyerPayoutAddress) {

        return buyerProtocol.createTrade(sellOffer, buyBtcAmount, buyerEscrowPubKey, buyerProfilePubKey, buyerPayoutAddress)
                .flatMap(this::writeTrade);
    }

    public void buyerSendPayment(String paymentReference) {
        buyerProtocol.sendPayment(selectedTrade.getValue(), paymentReference);
    }

    public void sellerConfirmPaymentReceived() {
        sellerProtocol.confirmPaymentReceived(selectedTrade.getValue());
    }

    public void requestArbitrate() {
        Trade trade = selectedTrade.getValue();

//        String profilePubKey = profileManager.getPubKeyProperty().getValue();
//        Boolean profileIsArbitrator = profileManager.getIsArbitratorProperty().getValue();

//        Trade.Role role = trade.role(profilePubKey, profileIsArbitrator);
//        if (role.equals(SELLER)) {
//            sellerProtocol.requestArbitrate(trade);
//        } else if (role.equals(BUYER)) {
//            buyerProtocol.requestArbitrate(trade);
//        }
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
        for (Trade trade : trades) {
            if (trade.getSellerEscrowPubKey().equals(sellerEscrowPubKey) && trade.status().equals(CREATED)) {
                return true;
            }
        }
        return false;
    }

    public boolean activeBuyerEscrowPubKey(String buyerEscrowPubKey) {
        for (Trade trade : trades) {
            if (trade.getBuyerEscrowPubKey().equals(buyerEscrowPubKey) && trade.status().equals(CREATED)) {
                return true;
            }
        }
        return false;
    }

    public void setSelectedTrade(Trade trade) {
        Platform.runLater(() -> {
            selectedTrade.setValue(trade);
        });
    }

    public Single<List<Trade>> singleTrades(String profilePubKey) {
        return tradeService.get(profilePubKey).retry().subscribeOn(Schedulers.io());
    }

    public Observable<Trade> watchUpdatedTrades() {
        return profileManager.retrieveMyProfile().toObservable()
                .flatMap(profile -> Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
                        .flatMap(tick -> tradeService.get(profile.getPubKey()).retry().toObservable())
                        .flatMapIterable(trades -> trades)
                        .map(trade -> Optional.ofNullable(handleTrade(profile, trade)))
                        .filter(Optional::isPresent).map(Optional::get));
        //.map(this::writeTrade));
    }

    private Single<List<Trade>> singleStoredTrades() {

        Single<List<Trade>> storedTrades = Single.create(source -> {
            // load stored trades
            List<Trade> trades = new ArrayList<>();
            File tradesDir = new File(TRADES_PATH);
            if (tradesDir.list() != null) {
                for (String tradeId : tradesDir.list()) {
                    try {
                        File tradeFile = new File(TRADES_PATH + tradeId + File.separator + "trade.json");
                        FileReader tradeReader = new FileReader(tradeFile);
                        Trade trade = JSON.std.beanFrom(Trade.class, tradeReader);
                        trades.add(trade);
                    } catch (IOException ioe) {
                        source.onError(ioe);
                    }
                }
            } else {
                tradesDir.mkdirs();
            }
            source.onSuccess(trades);
        });

        return storedTrades.subscribeOn(Schedulers.io());
    }

//    private void updateTrades() {

//        Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
//                .map(tick -> tradeService.get(profileManager.getPubKeyProperty().getValue()))
//                .retry()
//                .observeOn(JavaFxScheduler.platform())
//                .subscribe(c -> {
//                    try {
//                        List<Trade> trades = c.execute().body();
//                        if (trades != null) {
//                            receiveTrades(trades);
//                        }
//                    } catch (IOException ioe) {
//                        LOG.error(ioe.getMessage());
//                    }
//                });
//    }

//    private void receiveTrades(List<Trade> trades) {
//        for (Trade trade : trades) {
//            Trade updatedTrade = handleTrade(trade);
//            if (updatedTrade != null) {
//                writeTrade(updatedTrade);
//                updateTradesList(updatedTrade);
//            }
//        }
//    }

    private Trade handleTrade(Profile profile, Trade trade) {

        Trade currentTrade = getTrade(trade.getEscrowAddress());
        Trade updatedTrade = null;

        if (currentTrade == null || !currentTrade.status().equals(trade.status())) {

            String profilePubKey = profile.getPubKey();
            Boolean profileIsArbitrator = profile.getIsArbitrator();

            Trade.Role role = trade.role(profilePubKey, profileIsArbitrator);
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

            switch (trade.status()) {

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

        for (Trade trade : trades) {
            if (trade.getEscrowAddress().equals(escrowAddress)) {
                foundTrade = trade;
                break;
            }
        }

        return foundTrade;
    }

    private Single<Trade> writeTrade(Trade trade) {

        return Single.create(source -> {
            String tradePath = TRADES_PATH + trade.getEscrowAddress() + File.separator + "trade.json";

            try {
                File dir = new File(TRADES_PATH + trade.getEscrowAddress());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileWriter tradeWriter = new FileWriter(tradePath);
                tradeWriter.write(JSON.std.asString(trade));
                tradeWriter.flush();

                LOG.debug("Wrote local trade: {}", trade);

            } catch (IOException ioe) {
                LOG.error(ioe.getMessage());
                source.onError(ioe);
            }
            source.onSuccess(trade);
        });
    }

//    private void updateTradesList(Trade updatedTrade) {
//        int index = trades.indexOf(updatedTrade);
//        Platform.runLater(() -> {
//            if (index < 0) {
//                trades.add(updatedTrade);
//            } else {
//                trades.set(index, updatedTrade);
//            }
//        });
//    }

    public ObservableList<Trade> getTrades() {
        return trades;
    }

    public Trade getSelectedTrade() {
        return selectedTrade.get();
    }

    public ObjectProperty<Trade> selectedTradeProperty() {
        return selectedTrade;
    }
}