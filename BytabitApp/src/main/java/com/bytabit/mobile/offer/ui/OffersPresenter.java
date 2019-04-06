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

package com.bytabit.mobile.offer.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.ui.UiUtils;
import com.bytabit.mobile.offer.manager.OfferManager;
import com.bytabit.mobile.offer.model.Offer;
import com.bytabit.mobile.wallet.manager.WalletManager;
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
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Comparator;

import static com.bytabit.mobile.offer.model.Offer.OfferType.BUY;
import static com.bytabit.mobile.offer.model.Offer.OfferType.SELL;

@Slf4j
public class OffersPresenter {

    @Inject
    OfferManager offerManager;

    @Inject
    WalletManager walletManager;

    @FXML
    View offersView;

    @FXML
    CharmListView<Offer, String> offersListView;

    FloatingActionButton addOfferButton = new FloatingActionButton();

    public void initialize() {

        // setup view components

        addOfferButton.showOn(offersView);

        walletManager.getProfilePubKey().subscribe(p -> {

            offersListView.setCellFactory(view -> new CharmListCell<Offer>() {
                @Override
                public void updateItem(Offer o, boolean empty) {
                    super.updateItem(o, empty);
                    if (o != null && !empty) {
                        ListTile tile = new ListTile();
                        Offer.OfferType offerType = o.getOfferType();
                        if (o.getMakerProfilePubKey().equals(p)) {
                            // use different style if my offer
                            tile.getStyleClass().add("my-offer");
                        } else {
                            // swap offer type if not my offer
                            offerType = SELL.equals(o.getOfferType()) ? BUY : SELL;
                        }
                        String amount = String.format("%s @ %s %s per BTC", offerType.toString(), o.getPrice().toPlainString(), o.getCurrencyCode().toString());
                        String details = String.format("%s to %s %s via %s", o.getMinAmount(), o.getMaxAmount(), o.getCurrencyCode(), o.getPaymentMethod().displayName());
                        tile.textProperty().addAll(amount, details);
                        setText(null);
                        setGraphic(tile);
                    } else {
                        setText(null);
                        setGraphic(null);
                    }
                }
            });
        });

        offersListView.setComparator(new Comparator<Offer>() {
            @Override
            public int compare(Offer o1, Offer o2) {
                return o1.getPrice().compareTo(o2.getPrice());
            }
        });

        offersListView.setHeadersFunction(o -> o.getCurrencyCode().toString());

        // setup event observables

        walletManager.getWalletSynced()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(synced -> offersView.setDisable(!synced));

        JavaFxObservable.changesOf(offersView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapSingle(c -> offerManager.getStoredAndLoadedOffers())
                .observeOn(JavaFxScheduler.platform())
                .doOnError(UiUtils::showErrorDialog)
                .retry()
                .subscribe(offers -> {
                    setAppBar();
                    clearSelection();
                    offersListView.itemsProperty().setAll(offers);
                });

        Observable.create(source ->
                addOfferButton.setOnAction(source::onNext))
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(a ->
                        MobileApplication.getInstance().switchView(BytabitMobile.ADD_OFFER_VIEW)
                );

        JavaFxObservable.changesOf(offersListView.selectedItemProperty())
                .map(Change::getNewVal)
                .subscribe(offer -> {
                    MobileApplication.getInstance().switchView(BytabitMobile.OFFER_DETAILS_VIEW);
                    offerManager.setSelectedOffer(offer);
                });

        offerManager.getOffers()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(offers ->
                        offersListView.itemsProperty().setAll(offers)
                );

        offerManager.getUpdatedOffers()
                .observeOn(JavaFxScheduler.platform())
                .doOnError(UiUtils::showErrorDialog)
                .retry()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(trade -> log.debug("updated my offer: {}", trade));
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
        appBar.setTitleText("Offers");
    }

    private void clearSelection() {
        offersListView.selectedItemProperty().setValue(null);
    }
}