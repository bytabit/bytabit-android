package com.bytabit.mobile.profile.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.profile.manager.PaymentDetailsAction;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;

import javax.inject.Inject;

import static com.bytabit.mobile.profile.ui.PaymentDetailsEvent.Type.LIST_VIEW_SHOWING;

public class PaymentsPresenter {

    @Inject
    private ProfileManager profileManager;

    @FXML
    private View paymentsView;

    @FXML
    private CharmListView<PaymentDetails, String> paymentDetailsListView;

    private final EventLogger eventLogger = EventLogger.of(PaymentsPresenter.class);

    private FloatingActionButton addPaymentDetailsButton = new FloatingActionButton();

    public void initialize() {

        // setup view components

        paymentDetailsListView.setCellFactory((view) -> new CharmListCell<PaymentDetails>() {
            @Override
            public void updateItem(PaymentDetails paymentDetails, boolean empty) {
                super.updateItem(paymentDetails, empty);
                if (paymentDetails != null && !empty) {
                    ListTile tile = new ListTile();
                    String currencyCodeMethod = String.format("%s via %s",
                            paymentDetails.getCurrencyCode().name(),
                            paymentDetails.getPaymentMethod().displayName());
                    String details = String.format("%s", paymentDetails.getPaymentDetails());
                    tile.textProperty().addAll(currencyCodeMethod, details);
                    setText(null);
                    setGraphic(tile);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });

        paymentDetailsListView.setComparator((s1, s2) -> s2.getCurrencyCode().compareTo(s1.getCurrencyCode()));

        addPaymentDetailsButton.setText(MaterialDesignIcon.ADD.text);

        paymentsView.getLayers().add(addPaymentDetailsButton.getLayer());

        // setup event observables

        Observable<PaymentDetailsEvent> viewShowingEvents = JavaFxObservable.changesOf(paymentsView.showingProperty())
                .map(showing -> showing.getNewVal() ? PaymentDetailsEvent.listViewShowing() : PaymentDetailsEvent.listViewNotShowing());

        Observable<PaymentDetailsEvent> listItemChangedEvents = JavaFxObservable.changesOf(paymentDetailsListView.selectedItemProperty())
                .map(Change::getNewVal).map(PaymentDetailsEvent::listItemChanged);

        Observable<PaymentDetailsEvent> addButtonEvents = Observable.create(source ->
                addPaymentDetailsButton.setOnAction(source::onNext))
                .map(actionEvent -> PaymentDetailsEvent.listAddButtonPressed());

        Observable<PaymentDetailsEvent> paymentDetailsEvents = Observable.merge(viewShowingEvents,
                listItemChangedEvents, addButtonEvents)
                .compose(eventLogger.logEvents()).share();

        // transform events to actions

        Observable<PaymentDetailsAction> paymentDetailsActions =
                paymentDetailsEvents
                        .filter(e -> e.matches(LIST_VIEW_SHOWING))
                        .map(event -> {
                            switch (event.getType()) {
                                case LIST_VIEW_SHOWING:
                                    return PaymentDetailsAction.load();
                                default:
                                    throw new RuntimeException(String.format("Unexpected PaymentDetailsEvent.Type: %s", event.getType()));
                            }
                        }).startWith(PaymentDetailsAction.load());

        paymentDetailsActions.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .compose(eventLogger.logEvents())
                .subscribe(profileManager.getPaymentDetailsActions());

        // handle events

        paymentDetailsEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(event -> {
                    switch (event.getType()) {
                        case LIST_VIEW_SHOWING:
                            setAppBar();
                            break;
                        case LIST_VIEW_NOT_SHOWING:
                            break;
                        case LIST_ITEM_CHANGED:
                            break;
                        case LIST_ADD_BUTTON_PRESSED:
                            MobileApplication.getInstance().switchView(BytabitMobile.ADD_PAYMENT_VIEW);
                            break;
                        case DETAILS_VIEW_SHOWING:
                            break;
                        case DETAILS_VIEW_NOT_SHOWING:
                            break;
                        case DETAILS_ADD_BUTTON_PRESSED:
                            break;
                    }
                });

        // handle results

        profileManager.getPaymentDetailsResults().subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(result -> {
                    switch (result.getType()) {
                        case PENDING:
//                            paymentsView.setDisable(true);
                            break;
                        case LOADED:
                        case UPDATED:
                            paymentsView.setDisable(false);
                            updatePaymentDetailsList(result.getPaymentDetails());
                            break;
                        case ERROR:
                            break;
                    }
                });
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> {
            MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER);
        }));
        appBar.setTitleText("Payment Details");
    }

    private void updatePaymentDetailsList(PaymentDetails updated) {

        int index = paymentDetailsListView.itemsProperty().indexOf(updated);
        if (index > -1) {
            paymentDetailsListView.itemsProperty().remove(index);
        }
        paymentDetailsListView.itemsProperty().add(updated);
    }
}
