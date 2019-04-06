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

import com.bytabit.mobile.badge.manager.BadgeException;
import com.bytabit.mobile.badge.manager.BadgeManager;
import com.bytabit.mobile.badge.model.Badge;
import com.bytabit.mobile.common.ui.DecimalTextFieldFormatter;
import com.bytabit.mobile.common.ui.UiUtils;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Maybe;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BuyBadgePresenter {

    @Inject
    BadgeManager badgeManager;

    @FXML
    private View buyBadgeView;

    @FXML
    private ChoiceBox<Badge.BadgeType> badgeTypeChoiceBox;

    @FXML
    private ChoiceBox<CurrencyCode> currencyChoiceBox;

    @FXML
    private TextField priceBtcTextField;

    @FXML
    private TextField validFromTextField;

    @FXML
    private TextField validToTextField;

    @FXML
    private Button buyBadgeButton;

    private final DateFormat dateFormat;

    public BuyBadgePresenter() {
        dateFormat = new SimpleDateFormat("yyyy-MMM-dd");
        dateFormat.setTimeZone(TimeZone.getDefault());
    }

    public void initialize() {

        // setup view components
        setupViewComponents();

        // setup event observables
        handleShowing();
        handleAddBadge();
    }

    private void setupViewComponents() {

        badgeTypeChoiceBox.getItems().setAll(Badge.BadgeType.OFFER_MAKER);
        badgeTypeChoiceBox.getSelectionModel().clearAndSelect(0);

        currencyChoiceBox.getItems().setAll(CurrencyCode.values());
        currencyChoiceBox.getSelectionModel().clearAndSelect(0);

        priceBtcTextField.setTextFormatter(new DecimalTextFieldFormatter());
    }

    private void handleShowing() {

        JavaFxObservable.changesOf(buyBadgeView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(event -> {
                    setAppBar();
                    clearForm();
                });
    }

    private void handleAddBadge() {
        JavaFxObservable.actionEventsOf(buyBadgeButton)
                .flatMapMaybe(ea -> {

                    Badge.BadgeType badgeType = badgeTypeChoiceBox.selectionModelProperty().getValue().getSelectedItem();
                    CurrencyCode currencyCode = currencyChoiceBox.selectionModelProperty().getValue().getSelectedItem();

                    BigDecimal priceBtcAmount;
                    Date validFrom;
                    Date validTo;

                    try {
                        priceBtcAmount = new BigDecimal(priceBtcTextField.getText());
                    } catch (NumberFormatException nfe) {
                        return Maybe.error(new BadgeException("Max amount has invalid number format."));
                    }
                    try {
                        validFrom = dateFormat.parse(validFromTextField.getText());
                    } catch (ParseException pe) {
                        return Maybe.error(new BadgeException("Valid from date has invalid format."));
                    }
                    try {
                        validTo = dateFormat.parse(validToTextField.getText());
                    } catch (ParseException pe) {
                        return Maybe.error(new BadgeException("Valid to date has invalid format."));
                    }
                    return badgeManager.buyBadge(badgeType, currencyCode, priceBtcAmount, validFrom, validTo);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .doOnError(UiUtils::showErrorDialog)
                .retry()
                .subscribe(offer -> {
                    MobileApplication.getInstance().switchToPreviousView();
                });
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Buy Badge");
    }

    private void clearForm() {
        Date now = new Date();

        badgeManager.getStoredBadges().flattenAsObservable(b -> b)
                .filter(b -> b.getBadgeType().equals(Badge.BadgeType.OFFER_MAKER))
                .filter(b -> b.getValidFrom().compareTo(now) <=0  && b.getValidTo().compareTo(now) >= 0)
                .map(b -> b.getCurrencyCode()).distinct().toSortedList()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .doOnError(UiUtils::showErrorDialog)
                .retry()
                .subscribe(cl -> {
                    List<CurrencyCode> filtered = new ArrayList<>(Arrays.asList(CurrencyCode.values()));
                    filtered.removeAll(cl);
                    currencyChoiceBox.getItems().setAll(filtered);

                    if (currencyChoiceBox.getSelectionModel().isEmpty()) {
                        currencyChoiceBox.getSelectionModel().selectFirst();
                    }
                });

        if (badgeTypeChoiceBox.getSelectionModel().isEmpty()) {
            badgeTypeChoiceBox.getSelectionModel().selectFirst();
        }

        priceBtcTextField.setText("0.0025");

        Date validFrom = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(validFrom);
        c.add(Calendar.YEAR, 1);
        Date validTo = c.getTime();

        validFromTextField.setText(dateFormat.format(validFrom));
        validToTextField.setText(dateFormat.format(validTo));
    }
}
