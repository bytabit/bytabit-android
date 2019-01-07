/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.mobile.wallet.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.*;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Label;

import javax.inject.Inject;
import java.util.Locale;

public class WalletPresenter {

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

        // setup transaction list view
        transactionListView.setCellFactory(view -> new CharmListCell<TransactionWithAmt>() {
            @Override
            public void updateItem(TransactionWithAmt tx, boolean empty) {
                super.updateItem(tx, empty);
                if (tx != null && !empty) {
                    ListTile tile = new ListTile();
                    String amount = String.format("%s BTC, %s", tx.getTransactionAmt().toPlainString(), tx.getDate().toLocalDateTime());
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

        walletManager.getWalletsDownloadProgress()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(p -> downloadProgressBar.progressProperty().setValue(p));

        walletManager.getTradeUpdatedWalletTx()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(tx -> {
                    int index = transactionListView.itemsProperty().indexOf(tx);
                    if (index > -1) {
                        transactionListView.itemsProperty().remove(index);
                    }
                    transactionListView.itemsProperty().add(tx);
                    balanceAmountLabel.textProperty().setValue(tx.getWalletBalance().toFriendlyString());
                });

        walletManager.getTradeWalletConfig()
                .filter(c -> c.getMnemonicCode() != null || c.getCreationDate() != null)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> {
                    transactionListView.itemsProperty().clear();
                    balanceAmountLabel.textProperty().setValue("");
                });

        withdrawButton.setText(MaterialDesignIcon.REMOVE.text);
        depositButton.attachTo(withdrawButton, Side.LEFT);

        withdrawButton.showOn(walletView);

        Observable.create(source -> depositButton.setOnAction(source::onNext))
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> MobileApplication.getInstance().switchView(BytabitMobile.WALLET_DEPOSIT_VIEW));

        Observable.create(source -> withdrawButton.setOnAction(source::onNext))
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> MobileApplication.getInstance().switchView(BytabitMobile.WALLET_WITHDRAW_VIEW));

        JavaFxObservable.changesOf(walletView.showingProperty()).subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .filter(Change::getNewVal)
                .subscribe(c -> setAppBar());
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
        appBar.setTitleText("Wallet");
        appBar.getActionItems().add(MaterialDesignIcon.RESTORE.button(e ->
                MobileApplication.getInstance().switchView(BytabitMobile.WALLET_RESTORE_VIEW)));
        appBar.getActionItems().add(MaterialDesignIcon.INFO.button(e ->
                MobileApplication.getInstance().switchView(BytabitMobile.WALLET_BACKUP_VIEW)));
    }
}
