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

package com.bytabit.mobile.trade.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.ui.UiUtils;
import com.bytabit.mobile.offer.manager.OfferManager;
import com.bytabit.mobile.trade.manager.TradeManager;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
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

    @Inject
    WalletManager walletManager;

    @Inject
    OfferManager offerManager;

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
                    String amount = String.format("%s %s BTC @ %s %s", t.getRole().getAction(), t.getBtcAmount().toPlainString(), t.getPrice().toPlainString(), t.getCurrencyCode());
                    String details = String.format("%s for %s %s via %s", t.getStatus(), t.getPaymentAmount().toPlainString(),
                            t.getCurrencyCode(), t.getPaymentMethod().displayName());
                    tile.textProperty().addAll(amount, details);
                    setText(null);
                    setGraphic(tile);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });

        tradesListView.setComparator((t1, t2) -> t2.getCreatedTimestamp().compareTo(t1.getCreatedTimestamp()));

        tradesListView.setHeadersFunction(t -> t.getCurrencyCode().toString());

        // setup event observables

        JavaFxObservable.changesOf(tradesView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .flatMapSingle(c -> tradeManager.getStoredTrades())
                .observeOn(JavaFxScheduler.platform())
                .doOnError(UiUtils::showErrorDialog)
                .retry()
                .subscribe(tl -> {
                    tradesListView.itemsProperty().setAll(tl);
                    setAppBar();
                    clearSelection();
                });

        walletManager.getWalletSynced()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(synced -> tradesListView.setDisable(!synced));

        tradeManager.getUpdatedTrades()
                .mergeWith(offerManager.getAddedTrades())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .doOnError(UiUtils::showErrorDialog)
                .retry()
                .subscribe(updatedTrade -> {
                    int index = 0;
                    boolean found = false;
                    for (Trade existingTrade : tradesListView.itemsProperty()) {
                        if (existingTrade.getId().equals(updatedTrade.getId())) {
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