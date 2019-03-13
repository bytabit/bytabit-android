/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.mobile.trade.ui;

import com.bytabit.mobile.common.UiUtils;
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
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import static com.bytabit.mobile.trade.model.Trade.Status.COMPLETED;

@Slf4j
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

        tradeManager.getSelectedTrade()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::showTrade);

        JavaFxObservable.actionEventsOf(fundEscrowButton)
                .flatMapMaybe(ae -> tradeManager.fundEscrow())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .doOnError(UiUtils::showErrorDialog)
                .retry()
                .subscribe(trade -> {
                    log.debug("Trade escrow funded for trade {}", trade);
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(paymentSentButton)
                .flatMapMaybe(ae -> tradeManager.buyerSendPayment(paymentReferenceField.textProperty().get()))
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .doOnError(UiUtils::showErrorDialog)
                .retry()
                .subscribe(t -> {
                    log.debug("Buyer sent payment for trade {}", t);
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(paymentReceivedButton)
                .flatMapMaybe(ae -> tradeManager.sellerPaymentReceived())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(t -> {
                    log.debug("Seller payment received for trade {}", t);
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(arbitrateButton)
                .flatMapMaybe(ae -> tradeManager.requestArbitrate())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(t -> {
                    log.debug("Request arbitrate for trade {}", t);
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(refundSellerButton)
                .flatMapMaybe(ae -> tradeManager.arbitratorRefundSeller())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(t -> {
                    log.debug("Arbitrator refund seller for trade {}", t);
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(payoutBuyerButton)
                .flatMapMaybe(ae -> tradeManager.arbitratorPayoutBuyer())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(t -> {
                    log.debug("Arbitrator payout buyer for trade {}", t);
                    MobileApplication.getInstance().switchToPreviousView();
                });

        JavaFxObservable.actionEventsOf(cancelButton)
                .flatMapMaybe(ae -> tradeManager.cancelTrade())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(t -> {
                    log.debug("Cancel trade {}", t);
                    MobileApplication.getInstance().switchToPreviousView();
                });
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Trade Details");
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

        paymentReferenceField.setDisable(true);
        paymentReferenceField.setEditable(false);

        tradeStatusLabel.setText(trade.getStatus().toString());
        tradeRoleLabel.setText(trade.getRole().toString());

        // sell offer
        String currencyCode = trade.getCurrencyCode().toString();
        paymentMethodLabel.setText(trade.getPaymentMethod().displayName());
        paymentAmountCurrencyLabel.setText(currencyCode);
        priceCurrencyLabel.setText(currencyCode);
        priceLabel.setText(trade.getOffer().getPrice().toPlainString());

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
            case ACCEPTED:
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
                cancelButton.setDisable(false);
                break;
            case ACCEPTED:
                cancelButton.setDisable(false);
                fundEscrowButton.setDisable(false);
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
