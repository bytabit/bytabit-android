package com.bytabit.mobile.wallet.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.Event;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.*;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Label;

import javax.inject.Inject;
import java.util.Locale;

public class WalletPresenter {

    private final EventLogger eventLogger = EventLogger.of(WalletPresenter.class);

    @Inject
    WalletManager walletManager;

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

        Observable<WalletManager.BlockDownloadResult> blockDownloadResults =
                walletManager.getBlockDownloadResults()
                        .autoConnect(2)
                        .compose(eventLogger.logEvents())
                        .share();

        blockDownloadResults.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(WalletManager.BlockDownloadUpdate.class)
                .subscribe(result ->
                        downloadProgressBar.progressProperty().setValue(result.getPercent()));

        blockDownloadResults.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(WalletManager.BlockDownloadDone.class)
                .subscribe(result -> downloadProgressBar.progressProperty().setValue(1.0));

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

        walletManager.getTradeWalletTransactionResults().autoConnect()
                .subscribeOn(Schedulers.io())
                .compose(eventLogger.logEvents())
                .observeOn(JavaFxScheduler.platform())
                .ofType(WalletManager.TradeWalletUpdate.class)
                .subscribe(tr -> {
                    TransactionWithAmt transactionWithAmt = tr.getTransactionWithAmt();
                    int index = transactionListView.itemsProperty().indexOf(transactionWithAmt);
                    if (index > -1) {
                        transactionListView.itemsProperty().remove(index);
                    }
                    transactionListView.itemsProperty().add(transactionWithAmt);
                    balanceAmountLabel.textProperty().setValue(tr.getTransactionWithAmt().getWalletCoinBalance().toFriendlyString());
                });

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
                appBar.getActionItems().add(MaterialDesignIcon.RESTORE.button(e ->
                        MobileApplication.getInstance().switchView(BytabitMobile.WALLET_BACKUP_VIEW)));
                appBar.getActionItems().add(MaterialDesignIcon.INFO.button(e ->
                        MobileApplication.getInstance().switchView(BytabitMobile.WALLET_BACKUP_VIEW)));
            }

        });
    }

    // Event classes

    private interface PresenterEvent extends Event {
    }

    private class ViewShowing implements PresenterEvent {
    }

    private class ViewNotShowing implements PresenterEvent {
    }
}
