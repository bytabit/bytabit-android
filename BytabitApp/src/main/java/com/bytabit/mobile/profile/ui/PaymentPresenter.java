package com.bytabit.mobile.profile.ui;

import com.bytabit.mobile.common.Event;
import com.bytabit.mobile.common.EventLogger;
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

public class PaymentPresenter {

    @Inject
    ProfileManager profileManager;

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

        Observable<PresenterEvent> viewShowingEvents = JavaFxObservable.changesOf(paymentView.showingProperty())
                .map(showing -> showing.getNewVal() ? new ViewShowing() : new ViewNotShowing());

        Observable<PresenterEvent> currencySelectedEvents = JavaFxObservable.changesOf(currencyChoiceBox.valueProperty())
                .map(change -> new CurrencySelected(change.getNewVal()));

        Observable<PresenterEvent> addPaymentDetailButtonEvents = JavaFxObservable.actionEventsOf(addPaymentDetailButton)
                .map(actionEvent -> new AddButtonPressed(createPaymentDetailsFromUI()));

        Observable<PresenterEvent> paymentEvents = Observable.merge(viewShowingEvents,
                currencySelectedEvents, addPaymentDetailButtonEvents)
                .compose(eventLogger.logEvents()).share();

        // transform events to actions

        Observable<ProfileManager.ProfileAction> updatePaymentDetailsActions = paymentEvents
                .subscribeOn(Schedulers.io())
                .ofType(AddButtonPressed.class)
                .map(event -> profileManager.new UpdatePaymentDetails(event.paymentDetails));

//                .observeOn(JavaFxScheduler.platform())
//                .filter(e -> e.matches(DETAILS_ADD_BUTTON_PRESSED))
//                .map(event -> {
//                    switch (event.getType()) {
//                        case DETAILS_ADD_BUTTON_PRESSED:
//                            return PaymentDetailsAction.update(event.getPaymentDetails());
//                        default:
//                            throw new RuntimeException(String.format("Unexpected PaymentDetailsEvent.Type: %s", event.getType()));
//                    }
//                });

        // transform actions to results

        updatePaymentDetailsActions.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .compose(eventLogger.logEvents())
                .subscribe(profileManager.getActions());

        // handle events

        paymentEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(ViewShowing.class)
                .subscribe(event -> {
                    setAppBar();
                    clearForm();
                });

        paymentEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(CurrencySelected.class)
                .subscribe(event -> updatePaymentMethods(event.getCurrencyCode()));

        // handle results

        profileManager.getResults().subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(ProfileManager.PaymentDetailsUpdated.class)
                .subscribe(result ->
                        MobileApplication.getInstance().switchToPreviousView());
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

    // Event classes

    interface PresenterEvent extends Event {
    }

    private class ViewShowing implements PresenterEvent {
    }

    private class ViewNotShowing implements PresenterEvent {
    }

    private class CurrencySelected implements PresenterEvent {
        private final CurrencyCode currencyCode;

        public CurrencySelected(CurrencyCode currencyCode) {
            this.currencyCode = currencyCode;
        }

        public CurrencyCode getCurrencyCode() {
            return currencyCode;
        }
    }

    private class AddButtonPressed implements PresenterEvent {
        private final PaymentDetails paymentDetails;

        public AddButtonPressed(PaymentDetails paymentDetails) {
            this.paymentDetails = paymentDetails;
        }

        public PaymentDetails getPaymentDetails() {
            return paymentDetails;
        }
    }
}
