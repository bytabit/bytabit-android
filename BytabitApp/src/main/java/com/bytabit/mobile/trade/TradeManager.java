package com.bytabit.mobile.trade;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.trade.evt.*;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.fasterxml.jackson.jr.ob.JSON;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static com.bytabit.mobile.trade.model.Trade.Role.*;

public class TradeManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(TradeManager.class);

    @Inject
    BuyerProtocol buyerProtocol;

    @Inject
    SellerProtocol sellerProtocol;

    @Inject
    ArbitratorProtocol arbitratorProtocol;

    @Inject
    ProfileManager profileManager;

    @Inject
    WalletManager walletManager;

    private final TradeService tradeService;

    private Observable<TradeEvent> tradeEvents;

    private PublishSubject<TradeEvent> createdTradeEvents;

    private Observable<Trade> updatedTrades;

    private final ObjectProperty<Trade> selectedTrade = new SimpleObjectProperty<>();

    public final static String TRADES_PATH = AppConfig.getPrivateStorage().getPath() + File.separator +
            "trades" + File.separator;

    @Inject
    public TradeManager() {

        tradeService = new TradeService();
    }

    public void initialize() {

        if (tradeEvents == null) {

//            Observable<TradeEvent> readTrades = profileManager.loadMyProfile().toObservable()
//                    .flatMap(profile -> Observable.create(source -> {
//                        // load stored tradeEvents
//                        File tradesDir = new File(TRADES_PATH);
//                        if (!tradesDir.exists()) {
//                            tradesDir.mkdirs();
//                        } else if (tradesDir.list() != null) {
//                            for (String tradeId : tradesDir.list()) {
//                                try {
//                                    File tradeFile = new File(TRADES_PATH + tradeId + File.separator + "trade.json");
//                                    if (tradeFile.exists()) {
//                                        FileReader tradeReader = new FileReader(tradeFile);
//                                        Trade trade = JSON.std.beanFrom(Trade.class, tradeReader);
//                                        emitTradeEvents(source, profile, trade);
//                                    }
//                                } catch (IOException ioe) {
//                                    source.onError(ioe);
//                                }
//                            }
//                        }
//                        source.onComplete();
//                    }));
//
//            Observable<TradeEvent> receivedTrades = profileManager.loadMyProfile().toObservable()
//                    .flatMap(profile -> Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
//                            .flatMap(tick -> tradeService.get(profile.getPubKey())
//                                    .retryWhen(errors ->
//                                            errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS))
//                                    ).flattenAsObservable(tl -> tl))
//                            .flatMap(trade -> Observable.create(source -> emitTradeEvents(source, profile, trade))));

            createdTradeEvents = PublishSubject.create();

            //

            // post or patch all created trade events


//            tradeEvents = readTrades.concatWith(receivedTrades).mergeWith(createdTradeEvents)
//                    .distinct().subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
//                    .replay().autoConnect(3);
//
//            tradeEvents.filter(te -> te.getRole().equals(BUYER)).subscribe(te -> {
//                // buyer protocol
//                if (te instanceof BuyerCreated) {
//                    buyerProtocol.handleCreated((BuyerCreated) te);
//                    LOG.debug("Created trade event: {}", te);
//                }
//            });

            tradeEvents.filter(te -> te.getRole().equals(SELLER)).subscribe(te -> {
                // seller protocol
            });

            tradeEvents.filter(te -> te.getRole().equals(ARBITRATOR)).subscribe(te -> {
                // arbitrator protocol
            });

            updatedTrades = tradeEvents.scan(new HashMap<String, Trade>(), (ts, te) -> {
                Trade current = ts.get(te.getEscrowAddress());
                if (current == null && te instanceof BuyerCreated) {
                    BuyerCreated created = (BuyerCreated) te;
                    Trade trade = Trade.builder()
                            .escrowAddress(te.getEscrowAddress())
                            .sellOffer(created.getSellOffer())
                            .buyRequest(created.getBuyRequest())
                            .build();
                    ts.put(created.getEscrowAddress(), trade);
                } else if (current != null && te instanceof Funded) {
                    Funded funded = (Funded) te;
                    Trade trade = Trade.builder()
                            .escrowAddress(te.getEscrowAddress())
                            .sellOffer(current.sellOffer())
                            .buyRequest(current.buyRequest())
                            .paymentRequest(funded.getPaymentRequest())
                            .fundingTransactionWithAmt(funded.getTransactionWithAmt())
                            .build();
                    ts.put(trade.getEscrowAddress(), trade);
                } else if (current != null && te instanceof Paid) {
                    Paid paid = (Paid) te;
                    Trade trade = Trade.builder()
                            .escrowAddress(te.getEscrowAddress())
                            .sellOffer(current.sellOffer())
                            .buyRequest(current.buyRequest())
                            .paymentRequest(current.paymentRequest())
                            .fundingTransactionWithAmt(current.fundingTransactionWithAmt())
                            .payoutRequest(paid.getPayoutRequest())
                            .build();
                    ts.put(trade.getEscrowAddress(), trade);
                } else if (current != null && te instanceof Completed) {
                    Completed completed = (Completed) te;
                    Trade trade = Trade.builder()
                            .escrowAddress(te.getEscrowAddress())
                            .sellOffer(current.sellOffer())
                            .buyRequest(current.buyRequest())
                            .paymentRequest(current.paymentRequest())
                            .fundingTransactionWithAmt(current.fundingTransactionWithAmt())
                            .payoutRequest(current.payoutRequest())
                            .payoutCompleted(completed.getPayoutCompleted())
                            .payoutTransactionWithAmt(completed.getPayoutTransactionWithAmt())
                            .build();
                    ts.put(trade.getEscrowAddress(), trade);
                } else if (current != null && te instanceof Arbitrating) {
                    Arbitrating arbitrating = (Arbitrating) te;
                    Trade trade = Trade.builder()
                            .escrowAddress(te.getEscrowAddress())
                            .sellOffer(current.sellOffer())
                            .buyRequest(current.buyRequest())
                            .paymentRequest(current.paymentRequest())
                            .fundingTransactionWithAmt(current.fundingTransactionWithAmt())
                            .payoutRequest(current.payoutRequest())
                            .payoutCompleted(current.payoutCompleted())
                            .payoutTransactionWithAmt(current.payoutTransactionWithAmt())
                            .arbitrateRequest(arbitrating.getArbitrateRequest())
                            .build();
                    ts.put(trade.getEscrowAddress(), trade);
                } else {
                    LOG.error("Unable to update Trade with TradeEvent.");
                }

                return ts;
            }).flatMap(ts -> Observable.fromIterable(ts.values())).distinct()
                    .replay().autoConnect(2);

            // write updated trades to local storage
            updatedTrades.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .flatMap(trade -> writeTrade(trade).toObservable()).subscribe(trade -> {
                LOG.debug(String.format("Writing updated trade: %s", trade.toString()));
            });

//            tradeEvents = profileManager.loadMyProfile().toObservable().subscribeOn(Schedulers.io())
//                    .flatMap(profile -> readTrades.map(trade -> trade.isLoaded(true))
//                            .concatWith(receivedTrades).mergeWith(createdTradeEvents)
//                            .scan(new HashMap<String, Trade>(), (currentTrades, foundTrade) -> {
//                                for (Trade currentTrade : currentTrades.values()) {
//                                    currentTrade.isUpdated(false);
//                                    currentTrade.isLoaded(false);
//                                }
//                                Trade currentTrade = currentTrades.get(foundTrade.getEscrowAddress());
//                                if (currentTrade == null) {
//                                    foundTrade.isUpdated(true);
//                                    currentTrades.put(foundTrade.getEscrowAddress(), foundTrade);
//                                } else {
//                                    Trade updatedTrade = handleTrade(currentTrade, profile, foundTrade);
//                                    if (updatedTrade != null) {
//                                        updatedTrade.isUpdated(true);
//                                        currentTrades.put(updatedTrade.getEscrowAddress(), updatedTrade);
//                                    }
//                                }
//                                return currentTrades;
//                            }))
//                    .flatMap(currentTrades -> Observable.fromIterable(currentTrades.values()))
//                    .filter(Trade::isUpdated).publish().autoConnect(2);

//            tradeEvents.observeOn(Schedulers.io()).subscribe(this::writeTrade);

//            Observable<List<Trade>> storedTrades = Single.<List<Trade>>create(source -> {
//                // load stored tradeEvents
//                List<Trade> trades = new ArrayList<>();
//                File tradesDir = new File(TRADES_PATH);
//                if (!tradesDir.exists()) {
//                    tradesDir.mkdirs();
//                }
//                if (tradesDir.list() != null) {
//                    for (String tradeId : tradesDir.list()) {
//                        try {
//                            File tradeFile = new File(TRADES_PATH + tradeId + File.separator + "trade.json");
//                            if (tradeFile.exists()) {
//                                FileReader tradeReader = new FileReader(tradeFile);
//                                Trade trade = JSON.std.beanFrom(Trade.class, tradeReader);
//                                trades.add(trade);
//                            }
//                        } catch (IOException ioe) {
//                            source.onError(ioe);
//                        }
//                    }
//                } else {
//                    tradesDir.mkdirs();
//                }
//                source.onSuccess(trades);
//            }).toObservable();
//
//            Observable<List<Trade>> watchedTrades = profileManager.loadMyProfile().toObservable()
//                    .flatMap(profile -> Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
//                            .flatMap(tick -> tradeService.get(profile.getPubKey()).retry().toObservable()));
//
//            createdTradeEvents = PublishSubject.create();
//
//            tradeEvents = profileManager.loadMyProfile().toObservable().subscribeOn(Schedulers.io())
//                    .flatMap(profile -> storedTrades.concatWith(watchedTrades).mergeWith(createdTradeEvents)
//                            .scan(new HashMap<String, Trade>(), (currentTrades, foundTrades) -> {
//                                for (Trade currentTrade : currentTrades.values()) {
//                                    currentTrade.isUpdated(false);
//                                }
//                                for (Trade foundTrade : foundTrades) {
//                                    Trade updatedTrade = handleTrade(currentTrades, profile, foundTrade);
//                                    if (updatedTrade != null) {
//                                        updatedTrade.isUpdated(true);
//                                        currentTrades.put(updatedTrade.getEscrowAddress(), updatedTrade);
//                                    }
//                                }
//                                return currentTrades;
//                                //}).map(currentTrades -> (List<Trade>)new ArrayList<>(currentTrades.values()))).publish();
//                            }))
//                    .flatMap(currentTrades -> Observable.fromIterable(currentTrades.values()))
//                    .filter(Trade::isUpdated).publish().autoConnect();
//
//            tradeEvents.observeOn(Schedulers.io()).subscribe(this::writeTrade);
        }
    }

//    public BuyerCreated buyerCreateTrade(SellOffer sellOffer, BuyRequest buyRequest) {
//
//        String escrowAddress = walletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(),
//                sellOffer.getSellerEscrowPubKey(), buyRequest.getBuyerEscrowPubKey());
//        BuyerCreated created = new BuyerCreated(escrowAddress, BUYER, sellOffer, buyRequest);
//        createdTradeEvents.onNext(created);
//        return created;
//    }

//    public Single<Trade> buyerCreateTrade(SellOffer sellOffer, BigDecimal buyBtcAmount,
//                                          String buyerEscrowPubKey, String buyerProfilePubKey,
//                                          String buyerPayoutAddress) {
//
//        return buyerProtocol.buyerCreateTrade(sellOffer, buyBtcAmount, buyerEscrowPubKey, buyerProfilePubKey, buyerPayoutAddress);
//    }

    public void buyerSendPayment(String paymentReference) {
        buyerProtocol.sendPayment(selectedTrade.getValue(), paymentReference);
    }

    public void sellerConfirmPaymentReceived() {
        sellerProtocol.confirmPaymentReceived(selectedTrade.getValue());
    }

    public void requestArbitrate() {

//        profileManager.loadMyProfile().observeOn(JavaFxScheduler.platform()).subscribe(profile -> {
//            Trade trade = selectedTrade.getValue();
//
//            String profilePubKey = profile.getPubKey();
//            Boolean profileIsArbitrator = profile.isArbitrator();
//
//            Trade.Role role = trade.role(profilePubKey, profileIsArbitrator);
//            if (role.equals(SELLER)) {
//                sellerProtocol.requestArbitrate(trade);
//            } else if (role.equals(BUYER)) {
//                buyerProtocol.requestArbitrate(trade);
//            }
//        });
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

    public void setSelectedTrade(Trade trade) {
        Platform.runLater(() -> {
            selectedTrade.setValue(trade);
        });
    }

    public Single<List<Trade>> singleTrades(String profilePubKey) {
        return tradeService.get(profilePubKey).retry().subscribeOn(Schedulers.io());
    }

    private Trade handleTrade(Trade currentTrade, Profile profile, Trade foundTrade) {

//        Trade currentTrade = currentTrades.get(foundTrade.getEscrowAddress());
        Trade updatedTrade = null;

        if (currentTrade == null || !currentTrade.status().equals(foundTrade.status())) {

            String profilePubKey = profile.getPubKey();
            Boolean profileIsArbitrator = profile.isArbitrator();

            Trade.Role role = foundTrade.role(profilePubKey, profileIsArbitrator);
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

            switch (foundTrade.status()) {

                case CREATED:
                    //updatedTrade = currentTrade == null ? tradeProtocol.handleCreated(foundTrade) : null;
                    break;

                case FUNDED:
                    updatedTrade = currentTrade != null ? tradeProtocol.handleFunded(currentTrade, foundTrade) : foundTrade;
                    break;

                case PAID:
                    updatedTrade = currentTrade != null ? tradeProtocol.handlePaid(currentTrade, foundTrade) : foundTrade;
                    break;

                case COMPLETED:
                    updatedTrade = currentTrade != null ? tradeProtocol.handleCompleted(currentTrade, foundTrade) : foundTrade;
                    break;

                case ARBITRATING:
                    updatedTrade = currentTrade != null ? tradeProtocol.handleArbitrating(currentTrade, foundTrade) : foundTrade;
                    break;
            }
        }

        return updatedTrade;
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

    public Trade getSelectedTrade() {
        return selectedTrade.get();
    }

//    public ObjectProperty<Trade> selectedTradeProperty() {
//        return selectedTrade;
//    }

    public Observable<TradeEvent> getTradeEvents() {
        return tradeEvents;
    }

    public Observable<Trade> getUpdatedTrades() {
        return updatedTrades;
    }

//    public PublishSubject<TradeEvent> getCreatedTradeEvents() {
//        return createdTradeEvents;
//    }

    // convert trade into stream of trade events
    private void emitTradeEvents(ObservableEmitter<TradeEvent> source, Profile profile, Trade trade) {

        String escrowAddress = trade.getEscrowAddress();
        Trade.Role role = trade.role(profile.getPubKey(), profile.isArbitrator());

        source.onNext(new BuyerCreated(escrowAddress, role, trade.sellOffer(), trade.buyRequest()));
        if (trade.status().compareTo(Trade.Status.FUNDING) >= 0) {
            source.onNext(new Funded(escrowAddress, role, trade.paymentRequest(), trade.fundingTransactionWithAmt()));
        }
        if (trade.status().compareTo(Trade.Status.PAID) >= 0) {
            source.onNext(new Paid(escrowAddress, role, trade.payoutRequest()));
        }
        if (trade.status().compareTo(Trade.Status.ARBITRATING) == 0) {
            source.onNext(new Arbitrating(escrowAddress, role, trade.arbitrateRequest()));
        }
        if (trade.status().compareTo(Trade.Status.COMPLETING) >= 0) {
            source.onNext(new Completed(escrowAddress, role, trade.payoutCompleted(), trade.payoutTransactionWithAmt()));
        }
    }
}