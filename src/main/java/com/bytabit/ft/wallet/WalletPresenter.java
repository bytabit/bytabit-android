package com.bytabit.ft.wallet;

import com.bytabit.ft.FiatTraderMobile;
import com.bytabit.ft.wallet.model.TransactionUIModel;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ProgressBar;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.observables.JavaFxObservable;

import javax.inject.Inject;

public class WalletPresenter {

    Logger log = LoggerFactory.getLogger(WalletPresenter.class);

    @Inject
    private TradeWalletManager tradeWalletManager;

    @FXML
    private View wallet;

    @FXML
    private Label balanceAmountLabel;

    @FXML
    private CharmListView<TransactionUIModel, Integer> transactionListView;

    @FXML
    private ProgressBar downloadProgressBar;

    private FloatingActionButton depositButton = new FloatingActionButton();

    private FloatingActionButton withdrawButton = new FloatingActionButton();

    public void initialize() {
        wallet.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(FiatTraderMobile.MENU_LAYER)));
                appBar.setTitleText("Wallet");
                appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
                        System.out.println("Search")));

                withdrawButton.setText(MaterialDesignIcon.REMOVE.text);
                depositButton.attachTo(withdrawButton, Side.LEFT);

                wallet.getLayers().add(withdrawButton.getLayer());

                Observable<ActionEvent> withdrawEvents = JavaFxObservable.eventsOf(withdrawButton.getLayer().getChildren().get(0), ActionEvent.ACTION);

                withdrawEvents.subscribe(e -> {
                    log.debug("withdrawEvent {}", e);
                });

                withdrawEvents.subscribe(e -> {
                    log.debug("withdrawEvent2 {}", e);
                });

                Observable<ActionEvent> depositEvents = JavaFxObservable.eventsOf(withdrawButton.getLayer().getChildren().get(1), ActionEvent.ACTION);

                depositEvents.subscribe(e -> {
                    log.debug("depositEvent {}", e);
                });

                depositEvents.subscribe(e -> {
                    log.debug("depositEvent2 {}", e);
                });
            }
        });

    }
}
