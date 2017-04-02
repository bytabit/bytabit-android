package com.bytabit.mobile.profile;

import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
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
    private Label paymentDetailsLabel;

    @FXML
    private TextField paymentDetailsTextField;

    @FXML
    private Button addPaymentDetailButton;

    public void initialize() {

        LOG.debug("initialize add payment details presenter");

        paymentView.showingProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
                appBar.setTitleText("Add Payment Details");
            }

            paymentDetailsTextField.textProperty().setValue(null);
            currencyChoiceBox.getItems().setAll(CurrencyCode.values());
            currencyChoiceBox.getSelectionModel().select(0);
            currencyChoiceBox.requestFocus();
        });

        currencyChoiceBox.getSelectionModel().selectedItemProperty().addListener((obj, oldValue, currencyCode) -> {
            if (currencyCode != null) {
                paymentMethodChoiceBox.getItems().setAll(currencyCode.paymentMethods());
                paymentMethodChoiceBox.getSelectionModel().select(0);
            }
        });

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

        paymentMethodChoiceBox.getSelectionModel().selectedItemProperty().addListener((obj, oldValue, paymentMethod) -> {
            if (paymentMethod != null) {
                paymentDetailsTextField.setPromptText(paymentMethod.requiredDetails());
            } else {
                paymentDetailsTextField.setPromptText("");
            }
        });

        profileManager.newPaymentDetails().currencyCodeProperty().bind(currencyChoiceBox.valueProperty());
        profileManager.newPaymentDetails().paymentMethodProperty().bind(paymentMethodChoiceBox.valueProperty());
        profileManager.newPaymentDetails().paymentDetailsProperty().bind(paymentDetailsTextField.textProperty());

        addPaymentDetailButton.onActionProperty().setValue(e -> {
            profileManager.addPaymentDetails();
        });
    }
}
