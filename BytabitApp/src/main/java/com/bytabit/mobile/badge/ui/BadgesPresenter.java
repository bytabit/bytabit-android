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

package com.bytabit.mobile.badge.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.badge.manager.BadgeException;
import com.bytabit.mobile.badge.manager.BadgeManager;
import com.bytabit.mobile.badge.model.Badge;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Slf4j
public class BadgesPresenter {

    @Inject
    BadgeManager badgeManager;

    @FXML
    View badgesView;

    @FXML
    CharmListView<Badge, String> badgesListView;

    FloatingActionButton buyBadgeButton = new FloatingActionButton();

    private final DateFormat dateFormat;

    public BadgesPresenter() {
        dateFormat = new SimpleDateFormat("yyyy-MMM-dd");
        dateFormat.setTimeZone(TimeZone.getDefault());
    }

    public void initialize() {

        // setup view components

        buyBadgeButton.showOn(badgesView);

        badgesListView.setCellFactory(view -> new CharmListCell<Badge>() {
            @Override
            public void updateItem(Badge b, boolean empty) {
                super.updateItem(b, empty);
                if (b != null && !empty) {
                    ListTile tile = new ListTile();
                    String type = "";

                    switch (b.getBadgeType()) {
                        case BETA_TESTER:
                            type = String.format("%s", b.getBadgeType().toString());
                            break;
                        case OFFER_MAKER:
                            type = String.format("%s for %s", b.getBadgeType().toString(), b.getCurrencyCode().toString());
                            break;
                        case DETAILS_VERIFIED:
                            type = String.format("%s for %s via %s", b.getBadgeType().toString(), b.getCurrencyCode().toString(), b.getPaymentMethod().toString());
                            break;
                        default:
                            throw new BadgeException("Invalid badge type.");
                    }

                    String details = String.format("valid from %s%s", dateFormat.format(b.getValidFrom()),
                            b.getValidTo() != null ? " to " + dateFormat.format(b.getValidTo()) : "");

                    tile.textProperty().addAll(type, details);
                    setText(null);
                    setGraphic(tile);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });

        // setup event observables

        JavaFxObservable.changesOf(badgesView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> {
                    setAppBar();
                    clearSelection();
                });

        Observable.create(source ->
                buyBadgeButton.setOnAction(source::onNext))
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(a ->
                        MobileApplication.getInstance().switchView(BytabitMobile.BUY_BADGE)
                );

//        JavaFxObservable.changesOf(badgesListView.selectedItemProperty())
//                .map(Change::getNewVal)
//                .subscribe(offer -> {
//                    MobileApplication.getInstance().switchView(BytabitMobile.OFFER_DETAILS_VIEW);
//                    badgeManager.setSelectedOffer(offer);
//                });
//
//        Observable.concat(badgeManager.getLoadedOffers(), offerManager.getUpdatedOffers())
//                .subscribeOn(Schedulers.io())
//                .observeOn(JavaFxScheduler.platform())
//                .subscribe(offers ->
//                        badgesListView.itemsProperty().setAll(offers)
//                );
//
        badgeManager.getLoadedBadges().flattenAsObservable(b -> b)
                .concatWith(badgeManager.getCreatedBadges())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(offer ->
                        badgesListView.itemsProperty().add(offer)
                );
//
//        offerManager.getRemovedOffer()
//                .subscribeOn(Schedulers.io())
//                .observeOn(JavaFxScheduler.platform())
//                .subscribe(offer -> {
//                            int index = 0;
//                            boolean found = false;
//                            for (Offer existingOffer : badgesListView.itemsProperty()) {
//                                if (existingOffer.getId().equals(offer.getId())) {
//                                    found = true;
//                                    break;
//                                }
//                                index = index + 1;
//                            }
//                            if (found) {
//                                badgesListView.itemsProperty().remove(index);
//                            }
//                        }
//                );
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e ->
                MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Badges");
    }

    private void clearSelection() {
        badgesListView.selectedItemProperty().setValue(null);
    }
}