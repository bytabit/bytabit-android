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
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletPresenter {

    private static Logger LOG = LoggerFactory.getLogger(WalletPresenter.class);

//    @Inject
//    private TradeWalletManager tradeWalletManager;

    @FXML
    private View walletView;

    @FXML
    private Label balanceAmountLabel;

    @FXML
    private CharmListView<TransactionUIModel, Integer> transactionListView;

    @FXML
    private ProgressBar downloadProgressBar;

    private FloatingActionButton depositButton = new FloatingActionButton();

    private FloatingActionButton withdrawButton = new FloatingActionButton();

    public void initialize() {
        LOG.debug("initialize wallet presenter");
        walletView.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(FiatTraderMobile.MENU_LAYER)));
                appBar.setTitleText("Wallet");
                appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
                        System.out.println("Search")));

                withdrawButton.setText(MaterialDesignIcon.REMOVE.text);
                depositButton.attachTo(withdrawButton, Side.LEFT);

                walletView.getLayers().add(withdrawButton.getLayer());

//                EventObservables.getNavEvents().toObservable().filter(ne -> ne instanceof QuitEvent).subscribe(qe -> {
//                    tradeWalletManager.stopWallet();
//                });

//                tradeWalletManager.getWalletDownloadEvents().subscribe(e -> {
//                    log.debug("event: {}", e);
//                });
//
//                tradeWalletManager.getWalletDownloadEvents().subscribe(e -> {
//                    log.debug("event2: {}", e);
//                });
            }

            //tradeWalletManager.startWallet();

//                Observable<ActionEvent> withdrawEvents = JavaFxObservable
//                        .actionEventsOf(withdrawButton.getLayer().getChildren().get(0));

//                Observable<AppCommand> withdrawCommands = withdrawEvents.map(e -> new WithdrawCommand() {
//                });
//                appObservables.getAppCommands().add(withdrawCommands);
//
//                Observable<ActionEvent> depositEvents = JavaFxObservable
//                        .actionEventsOf(withdrawButton.getLayer().getChildren().get(1));
//
//                appObservables.getAppCommands().toObservable().subscribe(c -> {
//                   log.debug("command: {}", c);
//                });
//
//                withdrawEvents.subscribe(e -> {
//                    log.debug("withdrawEvent {}", e);
//                });
//
//                depositEvents.subscribe(e -> {
//                    log.debug("depositEvent {}", e);
//                });
//
//                withdrawEvents.subscribe(e -> {
//                    log.debug("withdrawEvent2 {}", e);
//                });
//
//                depositEvents.subscribe(e -> {
//                    log.debug("depositEvent2 {}", e);
//                });
//            }
        });

    }
}
