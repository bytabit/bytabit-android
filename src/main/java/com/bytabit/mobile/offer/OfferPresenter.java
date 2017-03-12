package com.bytabit.mobile.offer;

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

import java.math.BigDecimal;
import java.util.Optional;

public class OfferPresenter {

    private static Logger LOG = LoggerFactory.getLogger(OfferPresenter.class);

    @FXML
    private View offerView;

    @FXML
    private ChoiceBox<CurrencyCode> currencyChoiceBox;

    @FXML
    private ChoiceBox<PaymentMethod> paymentMethodChoiceBox;

    @FXML
    private TextField btcPriceTextField;

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

        LOG.debug("initialize add payment details presenter");

        offerView.showingProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
                appBar.setTitleText("Create Sell Offer");
            }
            paymentMethodChoiceBox.requestFocus();
        });

        currencyChoiceBox.getSelectionModel().selectedItemProperty().addListener((obj, oldValue, currencyCode) -> {
            paymentMethodChoiceBox.getItems().setAll(currencyCode.paymentMethods());
            paymentMethodChoiceBox.getSelectionModel().select(0);
            minTradeAmtCurrencyLabel.textProperty().setValue(currencyCode.name());
            maxTradeAmtCurrencyLabel.textProperty().setValue(currencyCode.name());
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

        currencyChoiceBox.getItems().setAll(CurrencyCode.values());
        currencyChoiceBox.getSelectionModel().select(0);

        btcPriceTextField

        addOfferButton.onActionProperty().setValue(e -> {
            CurrencyCode currencyCode = currencyChoiceBox.getSelectionModel().getSelectedItem();
            PaymentMethod paymentMethod = paymentMethodChoiceBox.getSelectionModel().getSelectedItem();
            BigDecimal price = new BigDecimal(btcPriceTextField.getText());
            if (currencyCode != null && paymentMethod != null && paymentDetails.length() > 0) {
                profileManager.setPaymentDetails(currencyCode, paymentMethod, paymentDetails);
            }
            Optional<String> addedPaymentDetails = profileManager.getPaymentDetails(currencyCode, paymentMethod);
            addedPaymentDetails.ifPresent(pd -> LOG.debug("added payment details: {}", pd));
        });
    }

}
