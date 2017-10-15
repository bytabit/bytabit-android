package com.bytabit.mobile.profile;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class PaymentsPresenter {

    private static Logger LOG = LoggerFactory.getLogger(PaymentsPresenter.class);

    @Inject
    private ProfileManager profileManager;

    @FXML
    private View paymentsView;

    @FXML
    private CharmListView<PaymentDetails, String> paymentDetailsListView;

    private FloatingActionButton addPaymentDetailsButton = new FloatingActionButton();

    public void initialize() {
        LOG.debug("initialize payment details presenter");

        // setup transaction list view
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

        addPaymentDetailsButton.setOnAction((e) ->
                MobileApplication.getInstance().switchView(BytabitMobile.ADD_PAYMENT_VIEW));

        paymentsView.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {

                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
                appBar.setTitleText("Payment Details");

                paymentDetailsListView.itemsProperty().setAll(profileManager.getPaymentDetails());
            }

        });
    }
}
