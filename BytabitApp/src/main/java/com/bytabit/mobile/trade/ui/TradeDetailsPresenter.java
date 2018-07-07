package com.bytabit.mobile.trade.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.trade.manager.TradeManager;
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

//    private final StringConverter<Trade.Status> statusStringConverter;
//
//    public TradeDetailsPresenter() {
//        this.statusStringConverter = new StringConverter<Trade.Status>() {
//
//            @Override
//            public String toString(Trade.Status status) {
//                return status.toString();
//            }
//
//            @Override
//            public Trade.Status fromString(String statusStr) {
//                return Trade.Status.valueOf(statusStr);
//            }
//        };
//    }

    public void initialize() {

        // setup view components

        // setup event observables

//        Observable<PresenterEvent> viewShowingEvents = JavaFxObservable.changesOf(tradeDetailsView.showingProperty())
//                .map(showing -> showing.getNewVal() ? new ViewShowing() : new ViewNotShowing());
//
//        Observable<FundButtonPressed> fundButtonPressedEvents = JavaFxObservable.actionEventsOf(fundEscrowButton)
//                .map(actionEvent -> new FundButtonPressed());

//        Observable<PresenterEvent> tradeDetailEvents = Observable.merge(viewShowingEvents,
//                fundButtonPressedEvents)
//                .doOnNext(progress -> log.debug("Download progress: {}", progress))
//                .share();

        // transform events to actions

        // handle events

//        tradeDetailEvents.subscribeOn(Schedulers.io())
//                .observeOn(JavaFxScheduler.platform())
//                .ofType(ViewShowing.class)
//                .subscribe(event -> {
//                    setAppBar();
//                });

//        Observable<TradeManager.TradeSelected> tradeSelectedObservable = tradeManager.getResults()
//                .ofType(TradeManager.TradeSelected.class)
//                .replay(1).refCount();
//
//        Observable<TradeManager.TradeUpdated> tradeUpdatedObservable = tradeManager.getResults()
//                .ofType(TradeManager.TradeUpdated.class);

        // events to actions

//        tradeDetailEvents.subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .ofType(FundButtonPressed.class)
//                .flatMap(fp -> tradeSelectedObservable.lastOrError().map(ts ->
//                        tradeManager.new FundTrade(ts.getTrade())).toObservable())
//                .compose(eventLogger.logEvents())
//                .subscribe(fe -> tradeManager.getActions().onNext(fe));

        // handle results

//        tradeSelectedObservable
//                .filter(ts -> ts.getTrade().status().compareTo(Trade.Status.CREATED) >= 0)
//                .subscribeOn(Schedulers.io())
//                .observeOn(JavaFxScheduler.platform())
//                .map(TradeManager.TradeSelected::getTrade)
//                .subscribe(this::showTrade);
//
//        tradeSelectedObservable.flatMap(s -> tradeUpdatedObservable
//                .filter(u -> u.getTrade().getEscrowAddress().equals(s.getTrade().getEscrowAddress())))
//                .subscribeOn(Schedulers.io())
//                .observeOn(JavaFxScheduler.platform())
//                .map(TradeManager.TradeUpdated::getTrade)
//                .subscribe(this::showTrade);

//        tradeDetailsView.showingProperty().addListener((observable, oldValue, newValue) -> {
//
//            // remove unusable buttons and disable all usable buttons
//            paymentReferenceField.setDisable(true);
//            paymentReferenceField.setEditable(false);

//            profileManager.loadOrCreateMyProfile().observeOn(JavaFxScheduler.platform()).subscribe(profile -> {
//                if (profile.isArbitrator()) {
//                    actionButtonsVBox.getChildren().remove(tradeButtonsFlowPane);
//                    refundSellerButton.setDisable(true);
//                    payoutBuyerButton.setDisable(true);
//                } else {
//                    actionButtonsVBox.getChildren().remove(arbitrateButtonsFlowPane);
//                    paymentSentButton.setDisable(true);
//                    paymentReceivedButton.setDisable(true);
//                    cancelButton.setDisable(true);
//                    arbitrateButton.setDisable(true);
//                }
//
//                if (newValue) {
//                    AppBar appBar = MobileApplication.getInstance().getAppBar();
//                    appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
//                    appBar.setTitleText("Trade Details");
//                    appBar.getActionItems().add(MaterialDesignIcon.INFO.button(e ->
//                            MobileApplication.getInstance().switchView(BytabitMobile.TRADE_DEV_INFO_VIEW)));
//
//                    Trade trade = tradeManager.getSelectedTrade();
//                    BigDecimal price = trade.getPrice();
//                    BigDecimal amount = trade.getBtcAmount();
//                    BigDecimal paymentAmount = price.multiply(amount);
////                String profilePubKey = profileManager.getPubKeyProperty().getValue();
////                Boolean arbitrator = profileManager.getIsArbitratorProperty().getValue();
//                    Trade.Role tradeRole = trade.role(profile.getPubKey(), profile.isArbitrator());
//
//                    //tradeStatusLabel.textProperty().bindBidirectional(trade.statusProperty(), statusStringConverter);
//                    tradeStatusLabel.textProperty().setValue(trade.status().toString());
//                    tradeRoleLabel.textProperty().setValue(tradeRole.toString());
//                    paymentMethodLabel.textProperty().setValue(trade.getCurrencyCode().toString() + " via " + trade.getPaymentMethod().displayName());
//                    paymentAmountLabel.textProperty().setValue(paymentAmount.toPlainString());
//                    paymentAmountCurrencyLabel.textProperty().setValue(trade.getCurrencyCode().toString());
//                    purchasedAmountLabel.textProperty().setValue(amount.toPlainString());
//                    priceLabel.textProperty().setValue(price.toPlainString());
//                    priceCurrencyLabel.textProperty().setValue(trade.getCurrencyCode().toString());
//                    paymentDetailsLabel.textProperty().setValue(null);
//                    paymentReferenceField.textProperty().setValue(null);
//                    arbitrateReasonLabel.textProperty().setValue(null);
//                    payoutReasonLabel.textProperty().setValue(null);
//                    tradeStatusLabel.textProperty().setValue(trade.status().toString());
//
//                    if (trade.status().equals(Trade.Status.FUNDED)) {
//                        paymentDetailsLabel.textProperty().setValue(trade.getPaymentDetails());
//                        if (tradeRole == Trade.Role.BUYER) {
//                            paymentReferenceField.setDisable(false);
//                            paymentReferenceField.setEditable(true);
//                            paymentSentButton.setDisable(false);
//                            cancelButton.setDisable(false);
//                        } else if (tradeRole == Trade.Role.SELLER) {
//                            arbitrateButton.setDisable(false);
//                        }
//                    } else if (trade.status().equals(Trade.Status.PAID)) {
//                        paymentDetailsLabel.textProperty().setValue(trade.getPaymentDetails());
//                        paymentReferenceField.textProperty().setValue(trade.getPaymentReference());
//                        arbitrateButton.setDisable(false);
//                        if (tradeRole == Trade.Role.BUYER) {
//                            paymentReceivedButton.setDisable(true);
//                        } else if (tradeRole == Trade.Role.SELLER) {
//                            paymentReceivedButton.setDisable(false);
//                        }
//                    } else if (trade.status().equals(Trade.Status.COMPLETED)) {
//                        paymentDetailsLabel.textProperty().setValue(trade.getPaymentDetails());
//                        if (trade.hasPayoutRequest()) {
//                            paymentReferenceField.textProperty().setValue(trade.getPaymentReference());
//                        }
//                        if (trade.hasArbitrateRequest()) {
//                            arbitrateReasonLabel.textProperty().setValue(trade.getArbitrationReason().toString());
//                        }
//                        if (trade.hasPayoutCompleted()) {
//                            payoutReasonLabel.textProperty().setValue(trade.getPayoutReason().toString());
//                        }
//                    } else if (trade.status().equals(Trade.Status.ARBITRATING)) {
//                        paymentDetailsLabel.textProperty().setValue(trade.getPaymentDetails());
//                        if (trade.hasPayoutRequest()) {
//                            paymentReferenceField.textProperty().setValue(trade.getPaymentReference());
//                        }
//                        arbitrateReasonLabel.textProperty().setValue(trade.getArbitrationReason().toString());
//                        if (tradeRole == Trade.Role.ARBITRATOR) {
//                            refundSellerButton.setDisable(false);
//                            if (trade.hasPayoutRequest()) {
//                                payoutBuyerButton.setDisable(false);
//                            }
//                        }
//                    }
//                }
//            });
//        });

//        paymentSentButton.setOnAction(e -> {
//            LOG.debug("paymentSentButton pressed");
//            if (paymentReferenceField.getText() != null && !paymentReferenceField.getText().isEmpty()) {
////                tradeManager.buyerSendPayment(paymentReferenceField.getText());
//                MobileApplication.getInstance().switchToPreviousView();
//            } else {
//                LOG.debug("No payment reference provided, skipped buyerSendPayment.");
//                // TODO notify user and/or don't enable paymentSentButton unless payment reference given
//            }
//        });

//        paymentReceivedButton.setOnAction(e -> {
//            LOG.debug("paymentReceivedButton pressed");
////            tradeManager.sellerConfirmPaymentReceived();
//            MobileApplication.getInstance().switchToPreviousView();
//        });

//        arbitrateButton.setOnAction(e -> {
//            LOG.debug("arbitrateButton pressed");
////            tradeManager.requestArbitrate();
//            MobileApplication.getInstance().switchToPreviousView();
//        });

//        refundSellerButton.setOnAction(e -> {
//            LOG.debug("refundSellerButton pressed");
////            tradeManager.arbitratorRefundSeller();
//            MobileApplication.getInstance().switchToPreviousView();
//        });

//        payoutBuyerButton.setOnAction(e -> {
//            LOG.debug("payoutBuyerButton pressed");
////            tradeManager.arbitratorPayoutBuyer();
//            MobileApplication.getInstance().switchToPreviousView();
//        });

//        cancelButton.setOnAction(e -> {
//            LOG.debug("cancelButton pressed");
////            tradeManager.buyerCancel();
//            MobileApplication.getInstance().switchToPreviousView();
//        });
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

    // Event classes

//    interface PresenterEvent extends Event {
//    }
//
//    private class ViewShowing implements PresenterEvent {
//    }
//
//    private class ViewNotShowing implements PresenterEvent {
//    }

//    private class FundButtonPressed implements PresenterEvent {
//    }
}
