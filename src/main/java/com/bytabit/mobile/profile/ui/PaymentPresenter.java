package com.bytabit.mobile.profile.ui;

import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.profile.manager.PaymentDetailsAction;
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
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import javax.inject.Inject;

import static com.bytabit.mobile.profile.ui.PaymentDetailsEvent.Type.DETAILS_ADD_BUTTON_PRESSED;

public class PaymentPresenter {

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

    private final EventLogger eventLogger = EventLogger.of(PaymentPresenter.class);

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

        // setup event observables

        Observable<PaymentDetailsEvent> viewShowingEvents = JavaFxObservable.changesOf(paymentView.showingProperty())
                .map(showing -> showing.getNewVal() ? PaymentDetailsEvent.detailsViewShowing() : PaymentDetailsEvent.detailsViewNotShowing());

        Observable<PaymentDetailsEvent> currencySelectedEvents = JavaFxObservable.changesOf(currencyChoiceBox.valueProperty())
                .map(change -> PaymentDetailsEvent.detailsCurrencySelected(change.getNewVal()));

        Observable<PaymentDetailsEvent> addPaymentDetailButtonEvents = JavaFxObservable.actionEventsOf(addPaymentDetailButton)
                .map(actionEvent -> PaymentDetailsEvent.detailsAddButtonPressed(createPaymentDetailsFromUI()));

        Observable<PaymentDetailsEvent> paymentDetailsEvents = Observable.merge(viewShowingEvents,
                currencySelectedEvents, addPaymentDetailButtonEvents)
                .compose(eventLogger.logEvents()).share();

        // transform events to actions

        Observable<PaymentDetailsAction> paymentDetailsActions = paymentDetailsEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .filter(e -> e.matches(DETAILS_ADD_BUTTON_PRESSED))
                .map(event -> {
                    switch (event.getType()) {
                        case DETAILS_ADD_BUTTON_PRESSED:
                            return PaymentDetailsAction.update(event.getPaymentDetails());
                        default:
                            throw new RuntimeException(String.format("Unexpected PaymentDetailsEvent.Type: %s", event.getType()));
                    }
                });

        // transform actions to results

        paymentDetailsActions.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .compose(eventLogger.logEvents())
                .subscribe(profileManager.getPaymentDetailsActions());

        // handle events

        paymentDetailsEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(event -> {
                    switch (event.getType()) {
                        case DETAILS_VIEW_SHOWING:
                            setAppBar();
                            clearForm();
                            break;
                        case DETAILS_CURRENCY_SELECTED:
                            updatePaymentMethods(event.getPaymentDetails().getCurrencyCode());
                            break;
                    }

                });

        // handle results

        profileManager.getPaymentDetailsResults().subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(result -> {
                    switch (result.getType()) {
                        case PENDING:
                            break;
                        case LOADED:
                            break;
                        case UPDATED:
                            MobileApplication.getInstance().switchToPreviousView();
                            break;
                        case ERROR:
                            break;
                    }
                });
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
