package com.bytabit.mobile.profile.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.profile.manager.PaymentDetailsManager;
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

public class PaymentsPresenter {

    @Inject
    PaymentDetailsManager paymentDetailsManager;

    @FXML
    private View paymentsView;

    @FXML
    private CharmListView<PaymentDetails, String> paymentDetailsListView;

    private FloatingActionButton addButton = new FloatingActionButton();

    public void initialize() {

        // setup view components

        paymentDetailsListView.setCellFactory(view -> new CharmListCell<PaymentDetails>() {
            @Override
            public void updateItem(PaymentDetails paymentDetails, boolean empty) {
                super.updateItem(paymentDetails, empty);
                if (paymentDetails != null && !empty) {
                    ListTile tile = new ListTile();
                    String currencyCodeMethod = String.format("%s via %s",
                            paymentDetails.getCurrencyCode().name(),
                            paymentDetails.getPaymentMethod().displayName());
                    String details = String.format("%s", paymentDetails.getDetails());
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

        addButton.setText(MaterialDesignIcon.ADD.text);

        paymentsView.getLayers().add(addButton.getLayer());

        // subscribe to observables

        JavaFxObservable.changesOf(paymentsView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> setAppBar());

        JavaFxObservable.changesOf(paymentDetailsListView.selectedItemProperty())
                .map(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(paymentDetails -> {
                    MobileApplication.getInstance().switchView(BytabitMobile.ADD_PAYMENT_VIEW);
                    paymentDetailsManager.setSelectedPaymentDetails(paymentDetails);
                });

        Observable.create(source -> addButton.setOnAction(source::onNext))
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(a -> {
                    MobileApplication.getInstance().switchView(BytabitMobile.ADD_PAYMENT_VIEW);
                    paymentDetailsManager.setSelectedPaymentDetails(new PaymentDetails(null, null, null));
                });

        Observable.concat(paymentDetailsManager.getLoadedPaymentDetails(),
                paymentDetailsManager.getUpdatedPaymentDetails())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(paymentDetails -> {
                    paymentsView.setDisable(false);
                    updatePaymentDetails(paymentDetails);
                });

        paymentDetailsManager.getRemovedPaymentDetails()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::removePaymentDetails);

    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
        appBar.setTitleText("Payment Details");
        paymentDetailsListView.selectedItemProperty().setValue(null);
    }

    private void updatePaymentDetails(PaymentDetails updated) {

        removePaymentDetails(updated);
        paymentDetailsListView.itemsProperty().add(updated);
    }

    private void removePaymentDetails(PaymentDetails updated) {

        PaymentDetails found = null;
        for (PaymentDetails paymentDetails : paymentDetailsListView.itemsProperty()) {
            if (paymentDetails.getCurrencyCode().equals(updated.getCurrencyCode()) &&
                    paymentDetails.getPaymentMethod().equals(updated.getPaymentMethod())) {
                found = paymentDetails;
                break;
            }
        }
        if (found != null) {
            paymentDetailsListView.itemsProperty().remove(found);
        }
    }
}
