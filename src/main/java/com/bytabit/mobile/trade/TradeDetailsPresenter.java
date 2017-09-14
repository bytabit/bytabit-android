package com.bytabit.mobile.trade;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
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
    private Label paymentMethodLabel;

    @FXML
    private Label paymentDetailsLabel;

    @FXML
    private Label arbitrateReasonLabel;

    @FXML
    private Label payoutReasonLabel;

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
    private Button cancelButton;

    @FXML
    private Button refundSellerButton;

    @FXML
    private Button payoutBuyerButton;

    @FXML
    private VBox actionButtonsVBox;

    @FXML
    private FlowPane tradeButtonsFlowPane;

    @FXML
    private FlowPane arbitrateButtonsFlowPane;

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

            // remove unusable buttons and disable all usable buttons
            paymentReferenceField.setDisable(true);
            paymentReferenceField.setEditable(false);

            if (profileManager.profile().isIsArbitrator()) {
                actionButtonsVBox.getChildren().remove(tradeButtonsFlowPane);
                refundSellerButton.setDisable(true);
                payoutBuyerButton.setDisable(true);
            } else {
                actionButtonsVBox.getChildren().remove(arbitrateButtonsFlowPane);
                paymentSentButton.setDisable(true);
                paymentReceivedButton.setDisable(true);
                cancelButton.setDisable(true);
                arbitrateButton.setDisable(true);
            }

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
                paymentMethodLabel.textProperty().setValue(trade.getSellOffer().getCurrencyCode().toString() + " via " + trade.getSellOffer().getPaymentMethod().displayName());
                paymentAmountLabel.textProperty().setValue(paymentAmount.toPlainString());
                paymentAmountCurrencyLabel.textProperty().setValue(trade.getSellOffer().getCurrencyCode().toString());
                purchasedAmountLabel.textProperty().setValue(amount.toPlainString());
                priceLabel.textProperty().setValue(price.toPlainString());
                priceCurrencyLabel.textProperty().setValue(trade.getSellOffer().getCurrencyCode().toString());
                paymentDetailsLabel.textProperty().setValue(null);
                paymentReferenceField.textProperty().setValue(null);
                arbitrateReasonLabel.textProperty().setValue(null);
                payoutReasonLabel.textProperty().setValue(null);
                tradeStatusLabel.textProperty().setValue(trade.getStatus().toString());

                if (trade.getStatus().equals(Trade.Status.FUNDED)) {
                    paymentDetailsLabel.textProperty().setValue(trade.getPaymentRequest().getPaymentDetails());
                    if (tradeRole == Trade.Role.BUYER) {
                        paymentReferenceField.setDisable(false);
                        paymentReferenceField.setEditable(true);
                        paymentSentButton.setDisable(false);
                        cancelButton.setDisable(false);
                    } else if (tradeRole == Trade.Role.SELLER) {
                        arbitrateButton.setDisable(false);
                    }
                } else if (trade.getStatus().equals(Trade.Status.PAID)) {
                    paymentDetailsLabel.textProperty().setValue(trade.getPaymentRequest().getPaymentDetails());
                    paymentReferenceField.textProperty().setValue(trade.getPayoutRequest().getPaymentReference());
                    arbitrateButton.setDisable(false);
                    if (tradeRole == Trade.Role.BUYER) {
                        paymentReceivedButton.setDisable(true);
                    } else if (tradeRole == Trade.Role.SELLER) {
                        paymentReceivedButton.setDisable(false);
                    }
                } else if (trade.getStatus().equals(Trade.Status.COMPLETED)) {
                    paymentDetailsLabel.textProperty().setValue(trade.getPaymentRequest().getPaymentDetails());
                    if (trade.getPayoutRequest() != null) {
                        paymentReferenceField.textProperty().setValue(trade.getPayoutRequest().getPaymentReference());
                    }
                    if (trade.getArbitrateRequest() != null) {
                        arbitrateReasonLabel.textProperty().setValue(trade.getArbitrateRequest().getReason().toString());
                    }
                    if (trade.getPayoutCompleted() != null) {
                        payoutReasonLabel.textProperty().setValue(trade.getPayoutCompleted().getReason().toString());
                    }
                } else if (trade.getStatus().equals(Trade.Status.ARBITRATING)) {
                    paymentDetailsLabel.textProperty().setValue(trade.getPaymentRequest().getPaymentDetails());
                    if (trade.getPayoutRequest() != null) {
                        paymentReferenceField.textProperty().setValue(trade.getPayoutRequest().getPaymentReference());
                    }
                    arbitrateReasonLabel.textProperty().setValue(trade.getArbitrateRequest().getReason().toString());
                    if (tradeRole == Trade.Role.ARBITRATOR) {
                        refundSellerButton.setDisable(false);
                        if (trade.getPayoutRequest() != null) {
                            payoutBuyerButton.setDisable(false);
                        }
                    }
                }
            }
        });

        paymentSentButton.setOnAction(e -> {
            LOG.debug("paymentSentButton pressed");
            if (paymentReferenceField.getText() != null && !paymentReferenceField.getText().isEmpty()) {
                tradeManager.requestPayout(paymentReferenceField.getText());
                MobileApplication.getInstance().switchToPreviousView();
            } else {
                LOG.debug("No payment reference provided, skipped requestPayout.");
                // TODO notify user and/or don't enable paymentSentButton unless payment reference given
            }
        });

        paymentReceivedButton.setOnAction(e -> {
            LOG.debug("paymentReceivedButton pressed");
            tradeManager.confirmPaymentReceived();
            MobileApplication.getInstance().switchToPreviousView();
        });

        arbitrateButton.setOnAction(e -> {
            LOG.debug("arbitrateButton pressed");
            tradeManager.requestArbitrate();
            MobileApplication.getInstance().switchToPreviousView();
        });

        refundSellerButton.setOnAction(e -> {
            LOG.debug("refundSellerButton pressed");
            tradeManager.refundSeller(PayoutCompleted.Reason.ARBITRATOR_SELLER_REFUND);
            MobileApplication.getInstance().switchToPreviousView();
        });

        payoutBuyerButton.setOnAction(e -> {
            LOG.debug("payoutBuyerButton pressed");
            tradeManager.arbitratorConfirmsPaymentReceived();
            MobileApplication.getInstance().switchToPreviousView();
        });

        cancelButton.setOnAction(e -> {
            LOG.debug("cancelButton pressed");
            tradeManager.refundSeller(PayoutCompleted.Reason.BUYER_SELLER_REFUND);
            MobileApplication.getInstance().switchToPreviousView();
        });
    }
}
