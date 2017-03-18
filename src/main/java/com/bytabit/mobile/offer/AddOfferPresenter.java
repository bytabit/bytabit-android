package com.bytabit.mobile.offer;

import com.bytabit.mobile.profile.PaymentMethodStringConverter;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.wallet.TradeWalletManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;

public class AddOfferPresenter {

    private static Logger LOG = LoggerFactory.getLogger(AddOfferPresenter.class);

    @Inject
    AddOfferManager addOfferManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    TradeWalletManager tradeWalletManager;

    @FXML
    private View addOfferView;

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

        LOG.debug("initialize add payment details presenter");

        addOfferView.showingProperty().addListener((observable, oldValue, newValue) -> {

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
            btcPriceCurrencyLabel.textProperty().setValue(currencyCode.name());
        });

        paymentMethodChoiceBox.setConverter(new PaymentMethodStringConverter());

        currencyChoiceBox.getItems().setAll(CurrencyCode.values());
        currencyChoiceBox.getSelectionModel().select(0);

        addOfferButton.onActionProperty().setValue(e -> {
            CurrencyCode currencyCode = currencyChoiceBox.getSelectionModel().getSelectedItem();
            PaymentMethod paymentMethod = paymentMethodChoiceBox.getSelectionModel().getSelectedItem();
            BigDecimal minAmount = new BigDecimal(minTradeAmtTextField.getText());
            BigDecimal maxAmount = new BigDecimal(maxTradeAmtTextField.getText());
            BigDecimal price = new BigDecimal(btcPriceTextField.getText());
            profileManager.readPubKey().ifPresent(sellerPubKey -> {

                String offerPubKey = tradeWalletManager.getFreshBase58PubKey();
                if (currencyCode != null &&
                        paymentMethod != null &&
                        minAmount.compareTo(BigDecimal.ZERO) >= 0 &&
                        maxAmount.compareTo(BigDecimal.ZERO) > 0 &&
                        price.compareTo(BigDecimal.ZERO) > 0) {
                    addOfferManager.createOffer(offerPubKey, sellerPubKey, currencyCode, paymentMethod, minAmount, maxAmount, price);
                }
            });
        });
    }

}
