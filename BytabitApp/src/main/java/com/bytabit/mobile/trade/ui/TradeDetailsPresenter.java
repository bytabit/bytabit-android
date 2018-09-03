package com.bytabit.mobile.trade.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.trade.manager.TradeManager;
import com.bytabit.mobile.trade.model.Trade;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
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

public class TradeDetailsPresenter {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    TradeManager tradeManager;

    @FXML
    View tradeDetailsView;

    @FXML
    Label tradeStatusLabel;

    @FXML
    Label tradeRoleLabel;

    @FXML
    Label paymentMethodLabel;

    @FXML
    Label paymentDetailsLabel;

    @FXML
    Label arbitrateReasonLabel;

    @FXML
    Label payoutReasonLabel;

    @FXML
    Label paymentAmountLabel;

    @FXML
    Label paymentAmountCurrencyLabel;

    @FXML
    Label purchasedAmountLabel;

    @FXML
    Label priceLabel;

    @FXML
    Label priceCurrencyLabel;

    @FXML
    Button fundEscrowButton;

    @FXML
    Button paymentSentButton;

    @FXML
    Button paymentReceivedButton;

    @FXML
    TextField paymentReferenceField;

    @FXML
    Button arbitrateButton;

    @FXML
    Button cancelButton;

    @FXML
    Button refundSellerButton;

    @FXML
    Button payoutBuyerButton;

    @FXML
    VBox actionButtonsVBox;

    @FXML
    FlowPane tradeButtonsFlowPane;

    @FXML
    FlowPane arbitrateButtonsFlowPane;

    private StringConverter<Trade.Status> statusStringConverter;

    public void initialize() {

        // setup view components

        this.statusStringConverter = new StringConverter<Trade.Status>() {

            @Override
            public String toString(Trade.Status status) {
                return status.toString();
            }

            @Override
            public Trade.Status fromString(String statusStr) {
                return Trade.Status.valueOf(statusStr);
            }
        };

        // setup event observables

        JavaFxObservable.changesOf(tradeDetailsView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> setAppBar());

        tradeManager.getLastSelectedTrade().autoConnect()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::showTrade);

        JavaFxObservable.actionEventsOf(paymentSentButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(ae -> {
                    tradeManager.buyerSendPayment(paymentReferenceField.textProperty().get());
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(paymentReceivedButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(actionEvent -> {
                    tradeManager.sellerConfirmPaymentReceived();
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(arbitrateButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(actionEvent -> {
                    tradeManager.requestArbitrate();
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(refundSellerButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(actionEvent -> {
                    tradeManager.arbitratorRefundSeller();
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(payoutBuyerButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(actionEvent -> {
                    tradeManager.arbitratorPayoutBuyer();
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(cancelButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(actionEvent -> {
                    tradeManager.cancelAndRefundSeller();
                    MobileApplication.getInstance().switchToPreviousView();
                });
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Trade Details");
        appBar.getActionItems().add(MaterialDesignIcon.INFO.button(e ->
                MobileApplication.getInstance().switchView(BytabitMobile.TRADE_DEV_INFO_VIEW)));

    }

    private void showTrade(Trade trade) {

        tradeStatusLabel.setText(trade.status().toString());
        tradeRoleLabel.setText(trade.getRole().toString());

        if (trade.getRole().equals(Trade.Role.ARBITRATOR)) {
            actionButtonsVBox.getChildren().remove(tradeButtonsFlowPane);
            refundSellerButton.setDisable(true);
            payoutBuyerButton.setDisable(true);
        } else {
            actionButtonsVBox.getChildren().remove(arbitrateButtonsFlowPane);
            fundEscrowButton.setDisable(true);
            paymentSentButton.setDisable(true);
            paymentReceivedButton.setDisable(true);
            cancelButton.setDisable(true);
            arbitrateButton.setDisable(true);
        }

        // sell offer
        String currencyCode = trade.getCurrencyCode().toString();
        paymentMethodLabel.setText(trade.getPaymentMethod().displayName());
        paymentAmountCurrencyLabel.setText(currencyCode);
        priceCurrencyLabel.setText(currencyCode);
        priceLabel.setText(trade.getPrice().toPlainString());

        // buy request
        purchasedAmountLabel.setText(trade.getBtcAmount().toPlainString());
        paymentAmountLabel.setText(trade.getBtcAmount().multiply(trade.getPrice()).toPlainString());

        paymentDetailsLabel.textProperty().setValue(null);
        paymentReferenceField.textProperty().setValue(null);
        arbitrateReasonLabel.textProperty().setValue(null);
        payoutReasonLabel.textProperty().setValue(null);

        if (trade.status().equals(Trade.Status.CREATED)) {
            paymentDetailsLabel.textProperty().setValue(trade.getPaymentDetails());
            if (trade.getRole() == Trade.Role.BUYER) {
                cancelButton.setDisable(false);
            } else if (trade.getRole() == Trade.Role.SELLER) {
                fundEscrowButton.setDisable(false);
                cancelButton.setDisable(false);
            }
        } else if (trade.status().equals(Trade.Status.FUNDING)) {
            paymentDetailsLabel.textProperty().setValue(trade.getPaymentDetails());
            cancelButton.setDisable(false);
            if (trade.getRole() == Trade.Role.BUYER) {
//                paymentReferenceField.setDisable(false);
//                paymentReferenceField.setEditable(true);
//                paymentSentButton.setDisable(false);
            } else if (trade.getRole() == Trade.Role.SELLER) {
                arbitrateButton.setDisable(false);
            }
        } else if (trade.status().equals(Trade.Status.FUNDED)) {
            paymentDetailsLabel.textProperty().setValue(trade.getPaymentDetails());
            cancelButton.setDisable(false);
            if (trade.getRole() == Trade.Role.BUYER) {
                paymentReferenceField.setDisable(false);
                paymentReferenceField.setEditable(true);
                paymentSentButton.setDisable(false);
            } else if (trade.getRole() == Trade.Role.SELLER) {
                arbitrateButton.setDisable(false);
            }
        } else if (trade.status().equals(Trade.Status.PAID)) {
            paymentDetailsLabel.textProperty().setValue(trade.getPaymentDetails());
            paymentReferenceField.textProperty().setValue(trade.getPaymentReference());
            arbitrateButton.setDisable(false);
            if (trade.getRole() == Trade.Role.BUYER) {
                paymentReceivedButton.setDisable(true);
            } else if (trade.getRole() == Trade.Role.SELLER) {
                paymentReceivedButton.setDisable(false);
            }
        } else if (trade.status().equals(Trade.Status.COMPLETED)) {
            paymentDetailsLabel.textProperty().setValue(trade.getPaymentDetails());
            if (trade.hasPayoutRequest()) {
                paymentReferenceField.textProperty().setValue(trade.getPaymentReference());
            }
            if (trade.hasArbitrateRequest()) {
                arbitrateReasonLabel.textProperty().setValue(trade.getArbitrationReason().toString());
            }
            if (trade.hasPayoutCompleted()) {
                payoutReasonLabel.textProperty().setValue(trade.getPayoutReason().toString());
            }
        } else if (trade.status().equals(Trade.Status.ARBITRATING)) {
            paymentDetailsLabel.textProperty().setValue(trade.getPaymentDetails());
            if (trade.hasPayoutRequest()) {
                paymentReferenceField.textProperty().setValue(trade.getPaymentReference());
            }
            arbitrateReasonLabel.textProperty().setValue(trade.getArbitrationReason().toString());
            if (trade.getRole() == Trade.Role.ARBITRATOR) {
                refundSellerButton.setDisable(false);
                if (trade.hasPayoutRequest()) {
                    payoutBuyerButton.setDisable(false);
                }
            }
        }
    }
}
