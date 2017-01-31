package com.bytabit.ft.wallet;

import com.bytabit.ft.FiatTraderMobile;
import com.bytabit.ft.nav.evt.QuitEvent;
import com.bytabit.ft.wallet.evt.TransactionUpdatedEvent;
import com.bytabit.ft.wallet.model.TransactionUIModel;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.*;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class WalletPresenter {

    private static Logger LOG = LoggerFactory.getLogger(WalletPresenter.class);

    @Inject
    private TradeWalletManager tradeWalletManager;

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

    @FXML
    public void initialize() {
        LOG.debug("initialize wallet presenter");

        // setup transaction list view
        transactionListView.setCellFactory((view) -> new CharmListCell<TransactionUIModel>() {
            @Override
            public void updateItem(TransactionUIModel tx, boolean empty) {
                super.updateItem(tx, empty);
                if (tx != null && !empty) {
                    ListTile tile = new ListTile();
                    String amount = String.format("%s, %tc", tx.getBtcAmt(), tx.getDate().toDate());
                    String details = String.format("%s (%d), Hash: %s",
                            tx.getConfidenceType(), tx.getDepth(), tx.getHash());
                    tile.textProperty().addAll(amount, details, tx.getMemo());
                    setText(null);
                    setGraphic(tile);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });

        withdrawButton.setText(MaterialDesignIcon.REMOVE.text);
        depositButton.attachTo(withdrawButton, Side.LEFT);

        walletView.getLayers().add(withdrawButton.getLayer());

        depositButton.setOnAction((e) ->
                MobileApplication.getInstance().switchView(FiatTraderMobile.DEPOSIT_VIEW));

        walletView.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {

                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(FiatTraderMobile.MENU_LAYER)));
                appBar.setTitleText("Wallet");
                appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
                        System.out.println("Search")));
            }

        });

        FiatTraderMobile.getNavEvents().filter(ne -> ne instanceof QuitEvent).subscribe(qe -> {
            LOG.debug("Got quit event");
            tradeWalletManager.stopWallet();
        });

        tradeWalletManager.getWalletEvents().subscribe(e -> {
            LOG.debug("wallet event : {}", e);
            if (e instanceof TransactionUpdatedEvent) {
                TransactionUpdatedEvent txe = TransactionUpdatedEvent.class.cast(e);
                TransactionUIModel txu = new TransactionUIModel(txe.getTx(), txe.getAmt());
                transactionListView.itemsProperty().add(txu);
            }
            //transactionListView.setItems(walletService.transactions());
        });

        tradeWalletManager.startWallet();
    }
}
