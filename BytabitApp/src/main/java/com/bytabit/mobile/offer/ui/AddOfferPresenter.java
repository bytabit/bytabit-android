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
import com.bytabit.mobile.badge.manager.BadgeException;
import com.bytabit.mobile.common.DecimalTextFieldFormatter;
import com.bytabit.mobile.common.UiUtils;
import com.bytabit.mobile.offer.manager.OfferManager;
import com.bytabit.mobile.offer.model.Offer;
import com.bytabit.mobile.profile.manager.PaymentDetailsManager;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javax.inject.Inject;
import java.math.BigDecimal;

import static com.bytabit.mobile.offer.model.Offer.OfferType.BUY;
import static com.bytabit.mobile.offer.model.Offer.OfferType.SELL;

public class AddOfferPresenter {

    @Inject
    OfferManager offerManager;

    @Inject
    PaymentDetailsManager paymentDetailsManager;

    @Inject
    ProfileManager profileManager;

    @FXML
    private View addOfferView;

    @FXML
    private ChoiceBox<Offer.OfferType> offerTypeChoiceBox;

    @FXML
    private ChoiceBox<CurrencyCode> currencyChoiceBox;

    @FXML
    private ChoiceBox<PaymentMethod> paymentMethodChoiceBox;

    @FXML
    private TextField btcPriceTextField;

    @FXML
    private Label btcPriceCurrencyLabel;

    @FXML
    private Button addOfferButton;

    @FXML
    private TextField minTradeAmtTextField;

    @FXML
    private Label minTradeAmtCurrencyLabel;

    @FXML
    private TextField maxTradeAmtTextField;

    @FXML
    private Label maxTradeAmtCurrencyLabel;

    public void initialize() {

        // setup view components
        setupViewComponents();

        // setup event observables
        handleShowing();
        handleAddOffer();
        handleCurrencyChoice();
        handleUpdatePaymentDetails();
        handleRemovePaymentDetails();
    }

    private void setupViewComponents() {

        offerTypeChoiceBox.getItems().setAll(BUY, SELL);
        offerTypeChoiceBox.getSelectionModel().clearAndSelect(0);

        paymentMethodChoiceBox.setConverter(new PaymentMethodStringConverter());

        minTradeAmtTextField.setTextFormatter(new DecimalTextFieldFormatter());
        maxTradeAmtTextField.setTextFormatter(new DecimalTextFieldFormatter());
        btcPriceTextField.setTextFormatter(new DecimalTextFieldFormatter());
    }

    private void handleShowing() {

        JavaFxObservable.changesOf(addOfferView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(event -> {
                    setAppBar();
                });
    }

    private void handleAddOffer() {
        JavaFxObservable.actionEventsOf(addOfferButton)
                .flatMapMaybe(ea -> {
                    Offer.OfferType offerType = offerTypeChoiceBox.selectionModelProperty().getValue().getSelectedItem();
                    CurrencyCode currencyCode = currencyChoiceBox.selectionModelProperty().getValue().getSelectedItem();
                    PaymentMethod paymentMethod = paymentMethodChoiceBox.selectionModelProperty().getValue().getSelectedItem();
                    BigDecimal maxAmount;
                    BigDecimal minAmount;
                    BigDecimal price;
                    try {
                        maxAmount = new BigDecimal(maxTradeAmtTextField.getText());
                    } catch (NumberFormatException nfe) {
                        throw new AddOfferPresenterException("Max amount has invalid number format.");
                    }
                    try {
                        minAmount = new BigDecimal(minTradeAmtTextField.getText());
                    } catch (NumberFormatException nfe) {
                        throw new AddOfferPresenterException("Min amount has invalid number format.");
                    }
                    try {
                        price = new BigDecimal(btcPriceTextField.getText());
                    } catch (NumberFormatException nfe) {
                        throw new AddOfferPresenterException("BTC price has invalid number format.");
                    }
                    return offerManager.createOffer(offerType, currencyCode, paymentMethod, minAmount, maxAmount, price);
                })
                .observeOn(JavaFxScheduler.platform())
                .doOnError(t -> {
                    if (t instanceof BadgeException) {
                        UiUtils.showErrorDialog(t);
                        MobileApplication.getInstance().switchView(BytabitMobile.BUY_BADGE);
                    }
                })
                .retry(t -> t instanceof BadgeException)
                .subscribeOn(Schedulers.io())
                .doOnError(UiUtils::showErrorDialog)
                .retry()
                .subscribe(offer -> {
                    MobileApplication.getInstance().switchToPreviousView();
                });
    }

    private void handleCurrencyChoice() {
        JavaFxObservable.changesOf(currencyChoiceBox.getSelectionModel().selectedItemProperty())
                .map(Change::getNewVal)
                .flatMap(cc -> paymentDetailsManager.getLoadedPaymentDetails()
                        .filter(p -> p.getCurrencyCode().equals(cc))
                        .map(PaymentDetails::getPaymentMethod).sorted()
                        .toList().toObservable())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(pm -> {
                    paymentMethodChoiceBox.itemsProperty().getValue().setAll(pm);
                    paymentMethodChoiceBox.getSelectionModel().selectFirst();
                });

        JavaFxObservable.changesOf(currencyChoiceBox.getSelectionModel().selectedItemProperty())
                .map(Change::getNewVal)
                .map(CurrencyCode::toString)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(cc -> {
                    minTradeAmtCurrencyLabel.setText(cc);
                    maxTradeAmtCurrencyLabel.setText(cc);
                    btcPriceCurrencyLabel.setText(cc);
                });
    }

    private void handleUpdatePaymentDetails() {

        Observable.concat(paymentDetailsManager.getLoadedPaymentDetails(),
                paymentDetailsManager.getUpdatedPaymentDetails())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(paymentDetails -> {
                    CurrencyCode cc = paymentDetails.getCurrencyCode();
                    PaymentMethod pm = paymentDetails.getPaymentMethod();
                    if (!currencyChoiceBox.getItems().contains(cc)) {
                        currencyChoiceBox.getItems().add(cc);
                    }
                    if (currencyChoiceBox.getSelectionModel().isEmpty()) {
                        currencyChoiceBox.getSelectionModel().selectFirst();
                    }
                    if (currencyChoiceBox.getSelectionModel().getSelectedItem().equals(cc) &&
                            !paymentMethodChoiceBox.getItems().contains(pm)) {
                        paymentMethodChoiceBox.getItems().add(pm);
                        if (paymentMethodChoiceBox.getSelectionModel().isEmpty()) {
                            paymentMethodChoiceBox.getSelectionModel().selectFirst();
                        }
                    }
                });
    }

    private void handleRemovePaymentDetails() {

        paymentDetailsManager.getRemovedPaymentDetails()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(pd -> {
                    CurrencyCode cc = pd.getCurrencyCode();
                    PaymentMethod pm = pd.getPaymentMethod();
                    if (currencyChoiceBox.getSelectionModel().getSelectedItem() != null &&
                            currencyChoiceBox.getSelectionModel().getSelectedItem().equals(cc)) {
                        paymentMethodChoiceBox.getItems().remove(pm);
                        if (paymentMethodChoiceBox.getItems().isEmpty()) {
                            currencyChoiceBox.getItems().remove(cc);
                            currencyChoiceBox.getSelectionModel().selectFirst();
                        } else if (paymentMethodChoiceBox.getSelectionModel().isEmpty()) {
                            paymentMethodChoiceBox.getSelectionModel().selectFirst();
                        }
                    }
                });
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Add Offer");
    }
}
