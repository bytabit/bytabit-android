package com.bytabit.mobile.profile.ui;

import com.bytabit.mobile.profile.manager.PaymentDetailsManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import javax.inject.Inject;

public class PaymentPresenter {

    @Inject
    PaymentDetailsManager paymentDetailsManager;

    @FXML
    private View paymentView;

    @FXML
    private ChoiceBox<CurrencyCode> currencyChoiceBox;

    @FXML
    private ChoiceBox<PaymentMethod> paymentMethodChoiceBox;

    @FXML
    private TextField paymentDetailsTextField;

    @FXML
    private Button addPaymentDetailButton;

    @FXML
    private Button removePaymentDetailButton;

    public void initialize() {

        // setup view components

        paymentMethodChoiceBox.setConverter(new StringConverter<PaymentMethod>() {
            @Override
            public String toString(PaymentMethod paymentMethod) {
                return paymentMethod.displayName();
            }

            @Override
            public PaymentMethod fromString(String displayName) {
                PaymentMethod found = null;
                for (PaymentMethod paymentMethod : PaymentMethod.values()) {
                    if (paymentMethod.displayName().equals(displayName)) {
                        found = paymentMethod;
                        break;
                    }
                }
                return found;
            }
        });

        // subscribe to observables

        JavaFxObservable.changesOf(paymentView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> {
                    setAppBar();
                    clearForm();
                });

        JavaFxObservable.changesOf(currencyChoiceBox.valueProperty())
                .map(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::updatePaymentMethods);

        JavaFxObservable.actionEventsOf(addPaymentDetailButton)
                .map(actionEvent -> getPaymentDetails())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(paymentDetailsManager::updatePaymentDetails);

        JavaFxObservable.actionEventsOf(addPaymentDetailButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(action -> MobileApplication.getInstance().switchToPreviousView());

        JavaFxObservable.actionEventsOf(removePaymentDetailButton)
                .map(actionEvent -> getPaymentDetails())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(paymentDetailsManager::removePaymentDetails);

        JavaFxObservable.actionEventsOf(removePaymentDetailButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(action -> MobileApplication.getInstance().switchToPreviousView());

        paymentDetailsManager.getSelectedPaymentDetails()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::setPaymentDetails);
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Add Payment Details");
    }

    private void clearForm() {
        paymentDetailsTextField.textProperty().setValue(null);
        currencyChoiceBox.getItems().setAll(CurrencyCode.values());
        currencyChoiceBox.getSelectionModel().select(0);
        currencyChoiceBox.requestFocus();
    }

    private void updatePaymentMethods(CurrencyCode currencyCode) {
        paymentMethodChoiceBox.getItems().setAll(currencyCode.paymentMethods());
        paymentMethodChoiceBox.getSelectionModel().select(0);
        paymentMethodChoiceBox.requestFocus();
    }

    private PaymentDetails getPaymentDetails() {
        return PaymentDetails.builder()
                .currencyCode(currencyChoiceBox.getValue())
                .paymentMethod(paymentMethodChoiceBox.getValue())
                .details(paymentDetailsTextField.textProperty().getValue())
                .build();
    }

    private void setPaymentDetails(PaymentDetails paymentDetails) {
        currencyChoiceBox.selectionModelProperty().getValue().select(paymentDetails.getCurrencyCode());
        paymentMethodChoiceBox.selectionModelProperty().getValue().select(paymentDetails.getPaymentMethod());
        paymentDetailsTextField.setText(paymentDetails.getDetails());
    }
}
