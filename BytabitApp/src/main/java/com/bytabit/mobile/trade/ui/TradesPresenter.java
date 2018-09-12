package com.bytabit.mobile.trade.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.trade.manager.TradeManager;
import com.bytabit.mobile.trade.model.Trade;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;

import javax.inject.Inject;

public class TradesPresenter {

    @Inject
    TradeManager tradeManager;

    @FXML
    private View tradesView;

    @FXML
    private CharmListView<Trade, String> tradesListView;

    public void initialize() {

        // setup view components

        tradesListView.setCellFactory(view -> new CharmListCell<Trade>() {
            @Override
            public void updateItem(Trade t, boolean empty) {
                super.updateItem(t, empty);
                if (t != null && !empty) {
                    ListTile tile = new ListTile();
                    String amount = String.format("%s %s BTC @ %s %s", t.role(), t.getBtcAmount(), t.getPrice(), t.getCurrencyCode());
                    String details = String.format("%s for %s %s via %s", t.status(), t.getBtcAmount().multiply(t.getPrice()),
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

        tradesListView.setComparator((t1, t2) -> t2.createdTimestamp().compareTo(t1.createdTimestamp()));

        // setup event observables

        JavaFxObservable.changesOf(tradesView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> {
                    setAppBar();
                    clearSelection();
                });

        tradeManager.getCreatedTrade()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(trade -> tradesListView.itemsProperty().add(trade));

        tradeManager.getUpdatedTrade()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(updatedTrade -> {
                    int index = 0;
                    boolean found = false;
                    for (Trade existingTrade : tradesListView.itemsProperty()) {
                        if (existingTrade.getEscrowAddress().equals(updatedTrade.getEscrowAddress())) {
                            found = true;
                            break;
                        }
                        index = index + 1;
                    }
                    if (found) {
                        tradesListView.itemsProperty().remove(index);
                    }
                    tradesListView.itemsProperty().add(updatedTrade);
                });

        JavaFxObservable.changesOf(tradesListView.selectedItemProperty())
                .map(Change::getNewVal)
                .subscribe(sellOffer -> {
                    MobileApplication.getInstance().switchView(BytabitMobile.TRADE_DETAILS_VIEW);
                    tradeManager.setSelectedTrade(sellOffer);
                });

    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
        appBar.setTitleText("Trades");
    }

    private void clearSelection() {
        tradesListView.selectedItemProperty().setValue(null);
    }
}