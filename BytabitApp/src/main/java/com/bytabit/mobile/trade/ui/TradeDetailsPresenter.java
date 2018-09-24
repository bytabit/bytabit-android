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

import javax.inject.Inject;

import static com.bytabit.mobile.trade.model.Trade.Status.COMPLETED;

public class TradeDetailsPresenter {

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

    public void initialize() {

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

        JavaFxObservable.actionEventsOf(fundEscrowButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(ae -> {
                    tradeManager.sellerFundEscrow().subscribe();
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(paymentSentButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(ae -> {
                    tradeManager.buyerSendPayment(paymentReferenceField.textProperty().get()).subscribe();
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(paymentReceivedButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(actionEvent -> {
                    tradeManager.sellerPaymentReceived().subscribe();
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(arbitrateButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(actionEvent -> {
                    tradeManager.requestArbitrate().subscribe();
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(refundSellerButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(actionEvent -> {
                    tradeManager.arbitratorRefundSeller().subscribe();
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(payoutBuyerButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(actionEvent -> {
                    tradeManager.arbitratorPayoutBuyer().subscribe();
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(cancelButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(actionEvent -> {
                    tradeManager.buyerRefundSeller().subscribe();
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

        showTradeDetails(trade);

        switch (trade.getRole()) {

            case BUYER:
                showDisabledTraderButtons();
                enableBuyerButtons(trade.getStatus());
                break;
            case SELLER:
                showDisabledTraderButtons();
                enableSellerButtons(trade.getStatus());
                break;
            case ARBITRATOR:
                showDisabledArbitratorButtons();
                enableArbitratorButtons(trade);
                break;
            default:
                disableAllActionButtons();
                break;
        }
    }

    private void showTradeDetails(Trade trade) {

        tradeStatusLabel.setText(trade.getStatus().toString());
        tradeRoleLabel.setText(trade.getRole().toString());

        // sell offer
        String currencyCode = trade.getCurrencyCode().toString();
        paymentMethodLabel.setText(trade.getPaymentMethod().displayName());
        paymentAmountCurrencyLabel.setText(currencyCode);
        priceCurrencyLabel.setText(currencyCode);
        priceLabel.setText(trade.getPaymentAmount().toPlainString());

        // buy request
        purchasedAmountLabel.setText(trade.getBtcAmount().toPlainString());
        paymentAmountLabel.setText(trade.getPaymentAmount().toPlainString());

        paymentDetailsLabel.textProperty().setValue(null);
        paymentReferenceField.textProperty().setValue(null);
        arbitrateReasonLabel.textProperty().setValue(null);
        payoutReasonLabel.textProperty().setValue(null);

        if (trade.hasPaymentRequest()) {
            paymentDetailsLabel.textProperty().setValue(trade.getPaymentDetails());
        }

        if (trade.hasPayoutRequest()) {
            paymentReferenceField.textProperty().setValue(trade.getPaymentReference());
        }

        if (trade.hasArbitrateRequest()) {
            arbitrateReasonLabel.textProperty().setValue(trade.getArbitrationReason().toString());
        }

        if (trade.hasPayoutCompleted()) {
            payoutReasonLabel.textProperty().setValue(trade.getPayoutReason().toString());
        }
    }

    private void disableAllActionButtons() {
        arbitrateButtonsFlowPane.setDisable(true);
        tradeButtonsFlowPane.setDisable(true);
    }

    private void showDisabledTraderButtons() {
        actionButtonsVBox.getChildren().remove(arbitrateButtonsFlowPane);
        fundEscrowButton.setDisable(true);
        paymentSentButton.setDisable(true);
        paymentReceivedButton.setDisable(true);
        cancelButton.setDisable(true);
        arbitrateButton.setDisable(true);
    }

    private void showDisabledArbitratorButtons() {
        actionButtonsVBox.getChildren().remove(tradeButtonsFlowPane);
        refundSellerButton.setDisable(true);
        payoutBuyerButton.setDisable(true);
    }

    private void enableBuyerButtons(Trade.Status status) {

        switch (status) {

            case CREATED:
            case FUNDING:
                cancelButton.setDisable(false);
                break;
            case FUNDED:
                cancelButton.setDisable(false);
                paymentReferenceField.setDisable(false);
                paymentReferenceField.setEditable(true);
                paymentSentButton.setDisable(false);
                break;
            case PAID:
                arbitrateButton.setDisable(false);
                break;
            case COMPLETING:
                arbitrateButton.setDisable(false);
                break;
            default:
                break;
        }
    }

    private void enableSellerButtons(Trade.Status status) {

        switch (status) {

            case CREATED:
                fundEscrowButton.setDisable(false);
                cancelButton.setDisable(false);
                break;
            case FUNDING:
            case FUNDED:
                arbitrateButton.setDisable(false);
                break;
            case PAID:
                arbitrateButton.setDisable(false);
                paymentReceivedButton.setDisable(false);
                break;
            case COMPLETING:
                arbitrateButton.setDisable(false);
                break;
            default:
                break;
        }
    }

    private void enableArbitratorButtons(Trade trade) {

        if (!COMPLETED.equals(trade.getStatus())) {

            refundSellerButton.setDisable(false);
            if (trade.hasPayoutRequest()) {
                payoutBuyerButton.setDisable(false);
            }
        }
    }
}
