package com.bytabit.mobile.trade;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.Event;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;

import javax.inject.Inject;

public class TradesPresenter {

    private final EventLogger eventLogger = EventLogger.of(TradesPresenter.class);

    @Inject
    TradeManager tradeManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    WalletManager walletManager;

    @FXML
    private View tradesView;

    @FXML
    private CharmListView<Trade, String> tradesListView;

    public void initialize() {

        // setup view components

        tradesListView.setCellFactory((view) -> new CharmListCell<Trade>() {
            @Override
            public void updateItem(Trade t, boolean empty) {
                super.updateItem(t, empty);
                if (t != null && !empty) {
                    ListTile tile = new ListTile();
                    String amount = String.format("%s BTC @ %s %s per BTC", t.getBtcAmount(), t.getPrice(), t.getCurrencyCode());
                    String details = String.format("for %s %s via %s", t.getBtcAmount().multiply(t.getPrice()),
                            t.getCurrencyCode(), t.getPaymentMethod().displayName());
                    tile.textProperty().addAll(amount, details, t.getEscrowAddress());
                    setText(null);
                    setGraphic(tile);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });

        // setup event observables

        Observable<PresenterEvent> viewShowingEvents = JavaFxObservable.changesOf(tradesView.showingProperty())
                .map(showing -> showing.getNewVal() ? new ViewShowing() : new ViewNotShowing());

        Observable<TradeSelected> tradeSelectedEvents = JavaFxObservable.changesOf(tradesListView.selectedItemProperty())
                .map(Change::getNewVal).map(TradeSelected::new);

        Observable<PresenterEvent> tradeEvents = Observable.merge(viewShowingEvents,
                tradeSelectedEvents)
                .compose(eventLogger.logEvents()).share();

        // transform events to actions

        Observable<ProfileManager.ProfileAction> profileActions = tradeEvents.ofType(ViewShowing.class)
                .map(v -> profileManager.new LoadProfile());

        profileActions.subscribe(profileManager.getActions());

        // handle events

        tradeEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(TradesPresenter.ViewShowing.class)
                .subscribe(event -> {
                    setAppBar();
                    clearSelection();
                });

        tradeEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(TradesPresenter.TradeSelected.class)
                .subscribe(event -> {
                    MobileApplication.getInstance().switchView(BytabitMobile.TRADE_DETAILS_VIEW);
                    tradeManager.getActions().onNext(tradeManager.new SelectTrade(event.getTrade()));
                });

        // handle results

        profileManager.getResults().ofType(ProfileManager.ProfileLoaded.class)
                .map(pl -> tradeManager.new GetTrades(pl.getProfile()))
                .subscribe(tradeManager.getActions());

        tradeManager.getResults().ofType(TradeManager.TradeLoaded.class)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .map(TradeManager.TradeLoaded::getTrade)
                .subscribe(this::updateTrade);

        tradeManager.getResults().ofType(TradeManager.TradeWritten.class)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .map(TradeManager.TradeWritten::getTrade)
                .subscribe(this::updateTrade);

        tradeManager.getResults().ofType(TradeManager.TradeReceived.class)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .map(TradeManager.TradeReceived::getReceivedTrade)
                .subscribe(this::updateTrade);


//        tradesView.showingProperty().addListener((obs, oldValue, newValue) -> {
//            if (newValue) {
//
//                AppBar appBar = MobileApplication.getInstance().getAppBar();
//                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
//                        MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
//                appBar.setTitleText("Trades");
//                appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
//                        System.out.println("Search")));
//            }
//        });

//        tradeManager.initialize();

//        tradeManager.watchUpdatedTrades().observeOn(JavaFxScheduler.platform()).forEach(this::updateTradesList);

//        tradeManager.singleTrades().addListener((ListChangeListener<Trade>) change -> {
//            while (change.next()) {
//                for (Trade trade : change.getAddedSubList()) {
//                    if (trade.createSellOffer().getSellerProfilePubKeyProperty()
//                            .equals(profileManager.profile().getPubKey())) {
//                        // TODO verify trade not yet funded
//                        try {
//                            walletManager.createEscrowWallet(trade.getEscrowAddress());
//
//                            String txHash = walletManager.fundEscrow(trade.getEscrowAddress(),
//                                    trade.getBuyRequest().getBtcAmount());
//
//                            String paymentDetails = profileManager.retrievePaymentDetails(trade.createSellOffer()
//                                    .getCurrencyCodeProperty(), trade.createSellOffer().getPaymentMethodProperty()).get();
//                            PaymentRequest paymentRequest = tradeManager.writePaymentRequest(trade, txHash, paymentDetails);
//
//                        } catch (InsufficientMoneyException e) {
//                            // TODO let user know not enough BTC in wallet
//                        }
//                    }
//                }
//            }
//        });

//        walletManager.getTradeWalletTransactions().addListener((ListChangeListener<TransactionWithAmt>) change -> {
//            while (change.next()) {
//                LOG.debug("Escrow transaction changed.");
//                for (TransactionWithAmt addedTx : change.getAddedSubList()) {
//                    // add transactions
//                    tradeManager.updateTradeWithTx(addedTx);
//                }
//            }
//        });

//        tradeManager.getUpdatedTrades().observeOn(JavaFxScheduler.platform())
//                .subscribe(trade -> {
//                    int index = tradesListView.itemsProperty().indexOf(trade);
//                    if (index > -1) {
//                        tradesListView.itemsProperty().set(index, trade);
//                    } else {
//                        tradesListView.itemsProperty().add(trade);
//                    }
//                });

//        tradesListView.itemsProperty().setValue(tradeManager.getTradeEvents());

//        tradesListView.selectedItemProperty().addListener((obs, oldValue, trade) -> {
//            if (trade != null) {
//                tradesListView.selectedItemProperty().setValue(null);
////                tradeManager.setSelectedTrade(trade);
//                MobileApplication.getInstance().switchView(BytabitMobile.TRADE_DETAILS_VIEW);
//            }
//        });
    }

//    private void updateTradesList(Trade updatedTrade) {
//        ObservableList<Trade> tradesObservableList = tradeManager.singleTrades();
//        int index = tradesObservableList.indexOf(updatedTrade);
//        if (index < 0) {
//            tradesObservableList.add(updatedTrade);
//        } else {
//            tradesObservableList.set(index, updatedTrade);
//        }
//    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
        appBar.setTitleText("Trades");
        appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
                System.out.println("Search")));
    }

    private void clearSelection() {
        tradesListView.selectedItemProperty().setValue(null);
    }

    private void updateTrade(Trade trade) {
        int index = tradesListView.itemsProperty().indexOf(trade);
        if (index > -1) {
            tradesListView.itemsProperty().remove(index);
        }
        tradesListView.itemsProperty().add(trade);
    }

    // Event classes

    interface PresenterEvent extends Event {
    }

    private class ViewShowing implements PresenterEvent {
    }

    private class ViewNotShowing implements PresenterEvent {
    }

    private class TradeChanged implements PresenterEvent {

        private final Trade trade;

        public TradeChanged(Trade trade) {
            this.trade = trade;
        }

        public Trade getTrade() {
            return trade;
        }
    }

    private class TradeSelected implements PresenterEvent {
        private final Trade trade;

        public TradeSelected(Trade trade) {
            this.trade = trade;
        }

        public Trade getTrade() {
            return trade;
        }
    }
}