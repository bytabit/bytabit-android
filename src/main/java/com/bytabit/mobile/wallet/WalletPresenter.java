package com.bytabit.mobile.wallet;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.nav.evt.QuitEvent;
import com.bytabit.mobile.wallet.evt.DownloadDone;
import com.bytabit.mobile.wallet.evt.DownloadProgress;
import com.bytabit.mobile.wallet.evt.TransactionUpdatedEvent;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
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
import rx.schedulers.JavaFxScheduler;

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
    private CharmListView<TransactionWithAmt, Integer> transactionListView;

    @FXML
    private ProgressBar downloadProgressBar;

    private FloatingActionButton depositButton = new FloatingActionButton();

    private FloatingActionButton withdrawButton = new FloatingActionButton();

    public void initialize() {
        LOG.debug("initialize wallet presenter");

        // setup transaction list view
        transactionListView.setCellFactory((view) -> new CharmListCell<TransactionWithAmt>() {
            @Override
            public void updateItem(TransactionWithAmt tx, boolean empty) {
                super.updateItem(tx, empty);
                if (tx != null && !empty) {
                    ListTile tile = new ListTile();
                    String amount = String.format("%s BTC, %tc", tx.getBtcAmt().toPlainString(), tx.getDate().toDate());
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
        transactionListView.setComparator((s1, s2) -> -1 * Integer.compare(s2.getDepth(), s1.getDepth()));

        withdrawButton.setText(MaterialDesignIcon.REMOVE.text);
        depositButton.attachTo(withdrawButton, Side.LEFT);

        walletView.getLayers().add(withdrawButton.getLayer());

        depositButton.setOnAction((e) ->
                MobileApplication.getInstance().switchView(BytabitMobile.DEPOSIT_VIEW));

        walletView.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {

                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
                appBar.setTitleText("Wallet");
                appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
                        System.out.println("Search")));
            }

        });

        BytabitMobile.getNavEvents().filter(ne -> ne instanceof QuitEvent).subscribe(qe -> {
            LOG.debug("Got quit event");
            tradeWalletManager.stopWallet();
        });

        tradeWalletManager.getWalletEvents().observeOn(JavaFxScheduler.getInstance())
                .subscribe(e -> {
                    LOG.debug("wallet event : {}", e);
                    if (e instanceof TransactionUpdatedEvent) {
                        TransactionUpdatedEvent txe = TransactionUpdatedEvent.class.cast(e);
                        TransactionWithAmt txu = new TransactionWithAmt(txe.getTx(), txe.getAmt());
                        Integer index = transactionListView.itemsProperty().indexOf(txu);
                        if (index > -1) {
                            transactionListView.itemsProperty().set(index, txu);
                        } else {
                            transactionListView.itemsProperty().add(txu);
                        }
                        balanceAmountLabel.setText(tradeWalletManager.getWalletBalance().toPlainString() + " BTC");
                    } else if (e instanceof DownloadDone) {
                        DownloadDone dde = DownloadDone.class.cast(e);
                        downloadProgressBar.setProgress(1.0);
                    } else if (e instanceof DownloadProgress) {
                        DownloadProgress dpe = DownloadProgress.class.cast(e);
                        downloadProgressBar.setProgress(dpe.getPct());
                    }
                });

        tradeWalletManager.startWallet();
    }
}
