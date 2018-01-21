package com.bytabit.mobile.profile;

import com.bytabit.mobile.profile.PaymentsResult.PaymentDetailsResult;
import com.bytabit.mobile.profile.action.PaymentDetailsAction;
import com.bytabit.mobile.profile.event.PaymentDetailsEvent;
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
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class PaymentPresenter {

    private static Logger LOG = LoggerFactory.getLogger(PaymentPresenter.class);

    @Inject
    private ProfileManager profileManager;

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

    public void initialize() {

        LOG.debug("initialize add payment details presenter");

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

        // setup event observables

        Observable<PaymentDetailsEvent> viewShowingEvents = JavaFxObservable.changesOf(paymentView.showingProperty())
                .map(showing -> showing.getNewVal() ? PaymentDetailsEvent.detailsViewShowing() : PaymentDetailsEvent.detailsViewNotShowing());

        Observable<PaymentDetailsEvent> currencySelectedEvents = JavaFxObservable.changesOf(currencyChoiceBox.valueProperty())
                .map(change -> PaymentDetailsEvent.detailsCurrencySelected(change.getNewVal()));

        Observable<PaymentDetailsEvent> addPaymentDetailButtonEvents = JavaFxObservable.actionEventsOf(addPaymentDetailButton)
                .map(actionEvent -> PaymentDetailsEvent.detailsAddButtonPressed(createPaymentDetailsFromUI()));

        Observable<PaymentDetailsEvent> paymentDetailsEvents = Observable.merge(viewShowingEvents,
                currencySelectedEvents, addPaymentDetailButtonEvents).publish().refCount();

        // transform events to actions

        Observable<PaymentDetailsAction> paymentDetailsActions = paymentDetailsEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform()).map(event -> {
                    switch (event.getType()) {

//                        case LIST_VIEW_SHOWING:
//                            break;
//                        case LIST_VIEW_NOT_SHOWING:
//                            break;
//                        case LIST_ITEM_CHANGED:
//                            break;
//                        case LIST_ADD_BUTTON_PRESSED:
//                            break;
//                        case DETAILS_VIEW_SHOWING:
//                            break;
//                        case DETAILS_VIEW_NOT_SHOWING:
//                            break;
                        case DETAILS_ADD_BUTTON_PRESSED:
                            return PaymentDetailsAction.add(event.getData());
//                        case DETAILS_BACK_BUTTON_PRESSED:
//                            break;
//                        case DETAILS_CURRENCY_SELECTED:
//                            break;
                        default:
                            throw new RuntimeException("Unexpected PaymentDetailsEvent.Type");
                    }
                });

        // transform actions to results

        Observable<PaymentDetailsResult> paymentDetailsResults = paymentDetailsActions
                .compose(profileManager.paymentDetailsActionTransformer());

        // handle events

        paymentDetailsEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(event -> {
                    switch (event.getType()) {
//                        case LIST_VIEW_SHOWING:
//                            break;
//                        case LIST_VIEW_NOT_SHOWING:
//                            break;
//                        case LIST_ITEM_CHANGED:
//                            break;
//                        case LIST_ADD_BUTTON_PRESSED:
//                            break;
                        case DETAILS_VIEW_SHOWING:
                            setAppBar();
                            clearForm();
                            break;
                        case DETAILS_CURRENCY_SELECTED:
                            updatePaymentMethods(event.getData().getCurrencyCode());
                            break;
//                        case DETAILS_VIEW_NOT_SHOWING:
//                            break;
//                        case DETAILS_ADD_BUTTON_PRESSED:
//                            break;
//                        case DETAILS_BACK_BUTTON_PRESSED:
//                            break;
                    }

                });

        // handle results

        paymentDetailsResults.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(result -> {
                    switch (result.getType()) {
                        case PENDING:
                            break;
                        case LOADED:
                            break;
                        case ADDED:
                        case UPDATED:
                            MobileApplication.getInstance().switchToPreviousView();
                            break;
                        case ERROR:
                            break;
                    }
                });

//        paymentView.showingProperty().addListener((observable, oldValue, newValue) -> {
//
//            if (newValue) {
//                AppBar appBar = MobileApplication.getInstance().getAppBar();
//                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
//                appBar.setTitleText("Add Payment Details");
//            }
//
//            paymentDetailsTextField.textProperty().setValue(null);
//            currencyChoiceBox.getItems().setAll(CurrencyCode.values());
//            currencyChoiceBox.getSelectionModel().select(0);
//            currencyChoiceBox.requestFocus();
//        });
//
//        currencyChoiceBox.getSelectionModel().selectedItemProperty().addListener((obj, oldValue, currencyCode) -> {
//            if (currencyCode != null) {
//                paymentMethodChoiceBox.getItems().setAll(currencyCode.paymentMethods());
//                paymentMethodChoiceBox.getSelectionModel().select(0);
//            }
//        });
//
//
//        paymentMethodChoiceBox.getSelectionModel().selectedItemProperty().addListener((obj, oldValue, paymentMethod) -> {
//            if (paymentMethod != null) {
//                paymentDetailsTextField.setPromptText(paymentMethod.requiredDetails());
//            } else {
//                paymentDetailsTextField.setPromptText("");
//            }
//        });

//        profileManager.getCurrencyCodeProperty().bind(currencyChoiceBox.valueProperty());
//        profileManager.getPaymentMethodProperty().bind(paymentMethodChoiceBox.valueProperty());
//        profileManager.getPaymentDetailsProperty().bind(paymentDetailsTextField.textProperty());

//        addPaymentDetailButton.onActionProperty().setValue(e -> {
//            profileManager.storePaymentDetails(currencyChoiceBox.valueProperty().get(), paymentMethodChoiceBox.valueProperty().get(),
//                    paymentDetailsTextField.textProperty().get()).observeOn(Schedulers.io()).subscribe();
//
//            MobileApplication.getInstance().switchToPreviousView();
//        });
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

    private PaymentDetails createPaymentDetailsFromUI() {
        return PaymentDetails.builder()
                .currencyCode(currencyChoiceBox.getValue())
                .paymentMethod(paymentMethodChoiceBox.getValue())
                .paymentDetails(paymentDetailsTextField.textProperty().getValue())
                .build();
    }
}
