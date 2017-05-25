package com.bytabit.mobile.trade;

import com.bytabit.mobile.offer.AddOfferPresenter;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.model.Trade;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;

public class TradeDetailsPresenter {

    private static Logger LOG = LoggerFactory.getLogger(AddOfferPresenter.class);

    @Inject
    TradeManager tradeManager;

    @Inject
    ProfileManager profileManager;

    @FXML
    private View tradeDetailsView;


    @FXML
    private Label tradeStatusLabel;

    @FXML
    private Label tradeRoleLabel;

    @FXML
    private Label tradeEscrowAddressLabel;

    @FXML
    private Label sellerEscrowPubKeyLabel;

    @FXML
    private Label sellerProfilePubKeyLabel;

    @FXML
    private Label arbitratorProfilePubKeyLabel;

    @FXML
    private Label paymentCurrencyLabel;

    @FXML
    private Label paymentMethodLabel;

    @FXML
    private Label paymentDetailsLabel;

    @FXML
    private Label paymentReferenceLabel;

    @FXML
    private Label paymentAmountLabel;

    @FXML
    private Label paymentAmountCurrencyLabel;

    @FXML
    private Label purchasedAmountLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Label priceCurrencyLabel;

    @FXML
    private Button paymentSentButton;

    @FXML
    private Button paymentReceivedButton;

    @FXML
    private TextField paymentReferenceField;

    enum TradeRole {
        BUYER, SELLER, TBD
    }

    public void initialize() {

        LOG.debug("initialize trade details presenter");

        tradeDetailsView.showingProperty().addListener((observable, oldValue, newValue) -> {

            paymentReceivedButton.visibleProperty().setValue(false);
            paymentSentButton.visibleProperty().setValue(false);
            paymentReferenceField.visibleProperty().setValue(false);
            
            TradeRole tradeRole = TradeRole.TBD;

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
                appBar.setTitleText("Trade Details");

                Trade trade = tradeManager.getViewTrade();
                BigDecimal price = trade.getSellOffer().getPrice();
                BigDecimal amount = trade.getBuyRequest().getBtcAmount();
                BigDecimal paymentAmount = price.multiply(amount);


                if (profileManager.profile().getPubKey().equals(trade.getBuyRequest().getBuyerProfilePubKey())) {
                    tradeRole = TradeRole.BUYER;
                } else if (profileManager.profile().getPubKey().equals(trade.getSellOffer().getSellerProfilePubKey())) {
                    tradeRole = TradeRole.SELLER;
                }

                tradeStatusLabel.textProperty().setValue("STARTED");
                tradeRoleLabel.textProperty().setValue(tradeRole.toString());
                tradeEscrowAddressLabel.textProperty().setValue(trade.getEscrowAddress());
                sellerEscrowPubKeyLabel.textProperty().setValue(trade.getSellOffer().getSellerEscrowPubKey());
                sellerProfilePubKeyLabel.textProperty().setValue(trade.getSellOffer().getSellerProfilePubKey());
                arbitratorProfilePubKeyLabel.textProperty().setValue(trade.getSellOffer().getArbitratorProfilePubKey());
                paymentCurrencyLabel.textProperty().setValue(trade.getSellOffer().getCurrencyCode().toString());
                paymentMethodLabel.textProperty().setValue(trade.getSellOffer().getPaymentMethod().displayName());
                paymentAmountLabel.textProperty().setValue(paymentAmount.toPlainString());
                paymentAmountCurrencyLabel.textProperty().setValue(trade.getSellOffer().getCurrencyCode().toString());
                purchasedAmountLabel.textProperty().setValue(amount.toPlainString());
                priceLabel.textProperty().setValue(price.toPlainString());
                priceCurrencyLabel.textProperty().setValue(trade.getSellOffer().getCurrencyCode().toString());
                paymentDetailsLabel.textProperty().setValue(null);
                paymentReferenceLabel.textProperty().setValue(null);

                if (trade.getPaymentRequest() != null && trade.getPaymentRequest().getFundingTxHash() != null) {
                    tradeStatusLabel.textProperty().setValue("FUNDED");
                    paymentDetailsLabel.textProperty().setValue(trade.getPaymentRequest().getPaymentDetails());
                    if (tradeRole == TradeRole.BUYER) {
                        paymentReceivedButton.visibleProperty().setValue(false);
                        paymentSentButton.visibleProperty().setValue(true);
                        paymentReferenceField.visibleProperty().setValue(true);
                    } else if (tradeRole == TradeRole.SELLER) {
                        paymentReceivedButton.visibleProperty().setValue(false);
                        paymentSentButton.visibleProperty().setValue(false);
                        paymentReferenceField.visibleProperty().setValue(false);
                    }
                }

                if (trade.getPayoutRequest() != null && trade.getPayoutRequest().getPaymentReference() != null) {
                    tradeStatusLabel.textProperty().setValue("PAYMENT SENT");
                    paymentReferenceLabel.textProperty().setValue(trade.getPayoutRequest().getPaymentReference());
                    if (tradeRole == TradeRole.BUYER) {
                        paymentReceivedButton.visibleProperty().setValue(false);
                        paymentSentButton.visibleProperty().setValue(false);
                        paymentReferenceField.visibleProperty().setValue(false);
                    } else if (tradeRole == TradeRole.SELLER) {
                        paymentReceivedButton.visibleProperty().setValue(true);
                        paymentSentButton.visibleProperty().setValue(false);
                        paymentReferenceField.visibleProperty().setValue(false);
                    }
                }
            }
        });

        paymentSentButton.setOnAction(e -> {
            LOG.debug("paymentSentButton pressed");
            tradeManager.createPayoutRequest(paymentReferenceField.getText());
        });
    }
}
