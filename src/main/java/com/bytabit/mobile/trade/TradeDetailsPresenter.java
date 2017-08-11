package com.bytabit.mobile.trade;

import com.bytabit.mobile.BytabitMobile;
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
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;

public class TradeDetailsPresenter {

    private static Logger LOG = LoggerFactory.getLogger(TradeDetailsPresenter.class);

    @Inject
    private TradeManager tradeManager;

    @Inject
    private ProfileManager profileManager;

    @FXML
    private View tradeDetailsView;

    @FXML
    private Label tradeStatusLabel;

    @FXML
    private Label tradeRoleLabel;

    @FXML
    private Label paymentCurrencyLabel;

    @FXML
    private Label paymentMethodLabel;

    @FXML
    private Label paymentDetailsLabel;

    @FXML
    private Label paymentReferenceLabel;

    @FXML
    private Label arbitrateReasonLabel;

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

    @FXML
    private Button arbitrateButton;

    @FXML
    private Button refundSellerButton;

    @FXML
    private Button payoutBuyerButton;

    StringConverter<Trade.Status> statusStringConverter = new StringConverter<Trade.Status>() {

        @Override
        public String toString(Trade.Status status) {
            return status.toString();
        }

        @Override
        public Trade.Status fromString(String statusStr) {
            return Trade.Status.valueOf(statusStr);
        }
    };

    public void initialize() {

        LOG.debug("initialize trade details presenter");

        tradeDetailsView.showingProperty().addListener((observable, oldValue, newValue) -> {

            paymentReceivedButton.visibleProperty().setValue(false);
            paymentSentButton.visibleProperty().setValue(false);
            paymentReferenceField.visibleProperty().setValue(false);
            refundSellerButton.visibleProperty().setValue(false);
            payoutBuyerButton.visibleProperty().setValue(false);
            arbitrateButton.visibleProperty().setValue(false);

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
                appBar.setTitleText("Trade Details");
                appBar.getActionItems().add(MaterialDesignIcon.INFO.button(e ->
                        MobileApplication.getInstance().switchView(BytabitMobile.TRADE_DEV_INFO_VIEW)));

                Trade trade = tradeManager.getSelectedTrade();
                BigDecimal price = trade.getSellOffer().getPrice();
                BigDecimal amount = trade.getBuyRequest().getBtcAmount();
                BigDecimal paymentAmount = price.multiply(amount);
                String profilePubKey = profileManager.profile().getPubKey();
                Boolean isArbitrator = profileManager.profile().isIsArbitrator();
                Trade.Role tradeRole = trade.getRole(profilePubKey, isArbitrator);

                //tradeStatusLabel.textProperty().bindBidirectional(trade.statusProperty(), statusStringConverter);
                tradeStatusLabel.textProperty().setValue(trade.statusProperty().toString());
                tradeRoleLabel.textProperty().setValue(tradeRole.toString());
                paymentCurrencyLabel.textProperty().setValue(trade.getSellOffer().getCurrencyCode().toString());
                paymentMethodLabel.textProperty().setValue(trade.getSellOffer().getPaymentMethod().displayName());
                paymentAmountLabel.textProperty().setValue(paymentAmount.toPlainString());
                paymentAmountCurrencyLabel.textProperty().setValue(trade.getSellOffer().getCurrencyCode().toString());
                purchasedAmountLabel.textProperty().setValue(amount.toPlainString());
                priceLabel.textProperty().setValue(price.toPlainString());
                priceCurrencyLabel.textProperty().setValue(trade.getSellOffer().getCurrencyCode().toString());
                paymentDetailsLabel.textProperty().setValue(null);
                paymentReferenceLabel.textProperty().setValue(null);
                arbitrateReasonLabel.textProperty().setValue(null);
                tradeStatusLabel.textProperty().setValue(trade.getStatus().toString());

                if (trade.getStatus().equals(Trade.Status.FUNDED)) {
                    paymentDetailsLabel.textProperty().setValue(trade.getPaymentRequest().getPaymentDetails());
                    if (tradeRole == Trade.Role.BUYER) {
                        paymentReceivedButton.visibleProperty().setValue(false);
                        paymentSentButton.visibleProperty().setValue(true);
                        paymentReferenceField.visibleProperty().setValue(true);
                    } else if (tradeRole == Trade.Role.SELLER) {
                        paymentReceivedButton.visibleProperty().setValue(false);
                        paymentSentButton.visibleProperty().setValue(false);
                        paymentReferenceField.visibleProperty().setValue(false);
                        arbitrateButton.visibleProperty().setValue(true);
                    }
                } else if (trade.getStatus().equals(Trade.Status.PAID)) {
                    paymentDetailsLabel.textProperty().setValue(trade.getPaymentRequest().getPaymentDetails());
                    paymentReferenceLabel.textProperty().setValue(trade.getPayoutRequest().getPaymentReference());
                    if (tradeRole == Trade.Role.BUYER) {
                        paymentReceivedButton.visibleProperty().setValue(false);
                        paymentSentButton.visibleProperty().setValue(false);
                        paymentReferenceField.visibleProperty().setValue(false);
                        arbitrateButton.visibleProperty().setValue(true);
                    } else if (tradeRole == Trade.Role.SELLER) {
                        paymentReceivedButton.visibleProperty().setValue(true);
                        paymentSentButton.visibleProperty().setValue(false);
                        paymentReferenceField.visibleProperty().setValue(false);
                        arbitrateButton.visibleProperty().setValue(true);
                    }
                } else if (trade.getStatus().equals(Trade.Status.COMPLETED)) {
                    paymentDetailsLabel.textProperty().setValue(trade.getPaymentRequest().getPaymentDetails());
                    if (trade.getPayoutRequest() != null) {
                        paymentReferenceLabel.textProperty().setValue(trade.getPayoutRequest().getPaymentReference());
                    }
                    if (trade.getArbitrateRequest() != null) {
                        arbitrateReasonLabel.textProperty().setValue(trade.getArbitrateRequest().getReason().toString());
                    }
                    if (tradeRole == Trade.Role.BUYER) {
                        paymentReceivedButton.visibleProperty().setValue(false);
                        paymentSentButton.visibleProperty().setValue(false);
                        paymentReferenceField.visibleProperty().setValue(false);
                    } else if (tradeRole == Trade.Role.SELLER) {
                        paymentReceivedButton.visibleProperty().setValue(false);
                        paymentSentButton.visibleProperty().setValue(false);
                        paymentReferenceField.visibleProperty().setValue(false);
                    }
                } else if (trade.getStatus().equals(Trade.Status.ARBITRATING)) {
                    paymentDetailsLabel.textProperty().setValue(trade.getPaymentRequest().getPaymentDetails());
                    if (trade.getPayoutRequest() != null) {
                        paymentReferenceLabel.textProperty().setValue(trade.getPayoutRequest().getPaymentReference());
                    }
                    arbitrateReasonLabel.textProperty().setValue(trade.getArbitrateRequest().getReason().toString());
                    arbitrateReasonLabel.visibleProperty().setValue(true);
                    //if (tradeRole == Trade.Role.BUYER && trade.getArbitrateRequest().getReason().equals(ArbitrateRequest.Reason.NO_PAYMENT)) {
                    //paymentSentButton.visibleProperty().setValue(true);
                    //paymentReferenceField.visibleProperty().setValue(true);
//                    } else if (tradeRole == Trade.Role.SELLER && trade.getArbitrateRequest().getReason().equals(ArbitrateRequest.Reason.NO_BTC)) {
//                        paymentReceivedButton.visibleProperty().setValue(true);
//                    } else
                    if (tradeRole == Trade.Role.ARBITRATOR) {
                        refundSellerButton.visibleProperty().setValue(true);
                        payoutBuyerButton.visibleProperty().setValue(true);
                    }
                }
            }
        });

        paymentSentButton.setOnAction(e -> {
            LOG.debug("paymentSentButton pressed");
            tradeManager.requestPayout(paymentReferenceField.getText());
        });

        paymentReceivedButton.setOnAction(e -> {
            LOG.debug("paymentReceivedButton pressed");
            tradeManager.confirmPaymentReceived();
        });

        arbitrateButton.setOnAction(e -> {
            LOG.debug("arbitrateButton pressed");
            tradeManager.requestArbitrate();
        });

        refundSellerButton.setOnAction(e -> {
            LOG.debug("refundSellerButton pressed");
            tradeManager.refundSeller();
        });

        payoutBuyerButton.setOnAction(e -> {
            LOG.debug("payoutBuyerButton pressed");
            tradeManager.payoutBuyer();
        });
    }
}
