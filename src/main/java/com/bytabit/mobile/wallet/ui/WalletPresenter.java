package com.bytabit.mobile.wallet.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.*;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Locale;

public class WalletPresenter {

    private static Logger LOG = LoggerFactory.getLogger(WalletPresenter.class);

    @Inject
    private WalletManager walletManager;

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

        walletManager.getBlockDownloadResults().observeOn(JavaFxScheduler.platform())
                .subscribe(bdr -> downloadProgressBar.progressProperty().setValue(bdr.getPercent()));

//        walletManager.getTradeWalletBalance().observeOn(JavaFxScheduler.platform())
//                .subscribe(wb -> balanceAmountLabel.textProperty().setValue(wb));

        // setup transaction list view
        transactionListView.setCellFactory((view) -> new CharmListCell<TransactionWithAmt>() {
            @Override
            public void updateItem(TransactionWithAmt tx, boolean empty) {
                super.updateItem(tx, empty);
                if (tx != null && !empty) {
                    ListTile tile = new ListTile();
                    String amount = String.format("%s BTC, %tc", tx.getTransactionCoinAmt().toPlainString(), tx.getDate().toDate());
                    String details = String.format(Locale.US, "%s (%d), Hash: %s", tx.getConfidenceType(), tx.getDepth(), tx.getHash());
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
//        transactionListView.itemsProperty().bindContent(walletManager.getTradeWalletTransactions());
        walletManager.getTradeWalletTransactionResults().observeOn(JavaFxScheduler.platform())
                .subscribe(tr -> {
                    TransactionWithAmt transactionWithAmt = tr.getTransactionWithAmt();
                    int index = transactionListView.itemsProperty().indexOf(transactionWithAmt);
                    if (index > -1) {
                        transactionListView.itemsProperty().remove(index);
                        //transactionListView.itemsProperty().set(index, transactionWithAmt);
                    } //else {
                    transactionListView.itemsProperty().add(transactionWithAmt);
                    balanceAmountLabel.textProperty().setValue(tr.getTransactionWithAmt().getWalletCoinBalance().toFriendlyString());
                    //}
                });

        withdrawButton.setText(MaterialDesignIcon.REMOVE.text);
        depositButton.attachTo(withdrawButton, Side.LEFT);

        walletView.getLayers().add(withdrawButton.getLayer());

        depositButton.setOnAction((e) ->
                MobileApplication.getInstance().switchView(BytabitMobile.DEPOSIT_VIEW));

        walletView.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                LOG.debug("Wallet view pending.");

                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
                appBar.setTitleText("Wallet");
                appBar.getActionItems().add(MaterialDesignIcon.RESTORE.button(e ->
                        MobileApplication.getInstance().switchView(BytabitMobile.WALLET_BACKUP_VIEW)));
                appBar.getActionItems().add(MaterialDesignIcon.INFO.button(e ->
                        MobileApplication.getInstance().switchView(BytabitMobile.WALLET_BACKUP_VIEW)));
            }

        });

//        walletView.setOnShown(e -> {
//            if (e.getEventType().equals(LifecycleEvent.SHOWN)) {
//                LOG.debug("Wallet view shown.");
//                walletManager.start();
//            }
//        });
//
//        balanceAmountLabel.textProperty().bind(walletManager.getTradeWalletBalance());
//        downloadProgressBar.progressProperty().bind(walletManager.downloadProgressProperty());
    }
}