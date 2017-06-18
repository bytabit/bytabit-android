package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.StringBigDecimalConverter;
import com.bytabit.mobile.profile.PaymentMethodStringConverter;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.profile.ProfileStringConverter;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.wallet.WalletManager;
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
import java.math.BigDecimal;

public class AddOfferPresenter {

    private static Logger LOG = LoggerFactory.getLogger(AddOfferPresenter.class);

    @Inject
    OfferManager offerManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    WalletManager tradeWalletManager;

    @FXML
    private View addOfferView;

    @FXML
    private ChoiceBox<CurrencyCode> currencyChoiceBox;

    @FXML
    private ChoiceBox<PaymentMethod> paymentMethodChoiceBox;

    @FXML
    private ChoiceBox<Profile> arbitratorChoiceBox;

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

        //profileManager.readProfiles();

        LOG.debug("initialize add offer presenter");

        StringConverter<BigDecimal> converter = new StringBigDecimalConverter();

        minTradeAmtTextField.textProperty().bindBidirectional(offerManager.newOffer().minAmountProperty(), converter);
        maxTradeAmtTextField.textProperty().bindBidirectional(offerManager.newOffer().maxAmountProperty(), converter);
        btcPriceTextField.textProperty().bindBidirectional(offerManager.newOffer().priceProperty(), converter);

        arbitratorChoiceBox.setConverter(new ProfileStringConverter());
        arbitratorChoiceBox.itemsProperty().setValue(profileManager.getArbitratorProfiles());
        arbitratorChoiceBox.getSelectionModel().selectedItemProperty().addListener((obj, oldValue, arbitrator) -> {
            offerManager.newOffer().arbitratorProfilePubKeyProperty().setValue(arbitrator.getPubKey());
        });
        addOfferView.showingProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
                appBar.setTitleText("Create Sell Offer");

                currencyChoiceBox.getItems().setAll(profileManager.currencyCodes());
                currencyChoiceBox.getSelectionModel().select(0);
            }
            paymentMethodChoiceBox.requestFocus();
        });

        currencyChoiceBox.getSelectionModel().selectedItemProperty().addListener((obj, oldValue, currencyCode) -> {
            if (currencyCode != null) {
                paymentMethodChoiceBox.getItems().setAll(profileManager.paymentMethods(currencyCode));
                paymentMethodChoiceBox.getSelectionModel().select(0);
                minTradeAmtCurrencyLabel.textProperty().setValue(currencyCode.name());
                maxTradeAmtCurrencyLabel.textProperty().setValue(currencyCode.name());
                btcPriceCurrencyLabel.textProperty().setValue(currencyCode.name());
                offerManager.newOffer().setCurrencyCode(currencyCode);
            }
        });

        currencyChoiceBox.getItems().setAll(profileManager.currencyCodes());
        currencyChoiceBox.getSelectionModel().select(0);

        paymentMethodChoiceBox.setConverter(new PaymentMethodStringConverter());
        paymentMethodChoiceBox.getSelectionModel().selectedItemProperty().addListener((obj, oldValue, paymentMethod) -> {
            offerManager.newOffer().setPaymentMethod(paymentMethod);
        });

        if (profileManager.profile().getPubKey() != null) {
            offerManager.newOffer().setSellerProfilePubKey(profileManager.profile().getPubKey());
        }

        addOfferButton.onActionProperty().setValue(e -> {

            if (tradeWalletManager.tradeWalletRunningProperty().getValue()) {
                offerManager.newOffer().setSellerEscrowPubKey(tradeWalletManager.getFreshBase58PubKey());
            }
            if (offerManager.newOffer().isComplete()) {
                offerManager.createOffer();
                MobileApplication.getInstance().switchToPreviousView();
            }
        });
    }
}
