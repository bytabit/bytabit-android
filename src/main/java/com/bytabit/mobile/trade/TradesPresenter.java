package com.bytabit.mobile.trade;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.model.PaymentRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.EscrowWalletManager;
import com.bytabit.mobile.wallet.TradeWalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import org.bitcoinj.core.InsufficientMoneyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class TradesPresenter {

    private static Logger LOG = LoggerFactory.getLogger(TradesPresenter.class);


    @Inject
    TradeManager tradeManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    EscrowWalletManager escrowWalletManager;

    @Inject
    TradeWalletManager tradeWalletManager;

    @FXML
    private View tradesView;

    @FXML
    private CharmListView<Trade, String> tradesListView;

    public void initialize() {
        tradesListView.setCellFactory((view) -> new CharmListCell<Trade>() {
            @Override
            public void updateItem(Trade t, boolean empty) {
                super.updateItem(t, empty);
                if (t != null && !empty) {
                    ListTile tile = new ListTile();
                    String amount = String.format("%s BTC @ %s %s per BTC", t.getBuyRequest().getBtcAmount(), t.getSellOffer().getPrice(), t.getSellOffer().getCurrencyCode());
                    String details = String.format("for %s %s via %s", t.getBuyRequest().getBtcAmount().multiply(t.getSellOffer().getPrice()),
                            t.getSellOffer().getCurrencyCode(), t.getSellOffer().getPaymentMethod().displayName());
                    tile.textProperty().addAll(amount, details, t.getEscrowAddress());
                    setText(null);
                    setGraphic(tile);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });

        tradesView.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {

                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
                appBar.setTitleText("Trades");
                appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
                        System.out.println("Search")));
            }

        });

        tradeManager.getTradesObservableList().addListener((ListChangeListener<Trade>) change -> {
            while (change.next()) {
                for (Trade trade : change.getAddedSubList()) {
                    if (trade.getSellOffer().getSellerProfilePubKey()
                            .equals(profileManager.profile().getPubKey())) {
                        // TODO verify trade not yet funded
                        try {
                            escrowWalletManager.watchTradeEscrowAddress(trade.getEscrowAddress());

                            String txHash = tradeWalletManager.fundEscrow(trade.getEscrowAddress(),
                                    trade.getBuyRequest().getBtcAmount());

                            String paymentDetails = profileManager.retrievePaymentDetails(trade.getSellOffer()
                                    .getCurrencyCode(), trade.getSellOffer().getPaymentMethod()).get();
                            PaymentRequest paymentRequest = tradeManager.createPaymentRequest(trade, txHash, paymentDetails);

                        } catch (InsufficientMoneyException e) {
                            // TODO let user know not enough BTC in wallet
                        }
                    }
                }
            }
        });

        escrowWalletManager.getTransactions().addListener((ListChangeListener<TransactionWithAmt>) change -> {
            while (change.next()) {
                LOG.debug("Escrow transactions changed.");
                for (TransactionWithAmt removedTx : change.getRemoved()) {
                    // remove transactions
                    tradeManager.removeTradeTx(removedTx);
                }
                for (TransactionWithAmt addedTx : change.getAddedSubList()) {
                    // add transactions
                    tradeManager.addTradeTx(addedTx);
                }
            }
        });

        tradesListView.itemsProperty().setValue(tradeManager.getTradesObservableList());
        tradesListView.selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            Trade viewTrade = tradeManager.getViewTrade();
            viewTrade.setEscrowAddress(newValue.getEscrowAddress());
            viewTrade.setSellOffer(newValue.getSellOffer());
            viewTrade.setBuyRequest(newValue.getBuyRequest());
            viewTrade.setPaymentRequest(newValue.getPaymentRequest());
            viewTrade.setPayoutRequest(newValue.getPayoutRequest());
            viewTrade.setPayoutTxHash(newValue.getPayoutTxHash());
            MobileApplication.getInstance().switchView(BytabitMobile.TRADE_DETAILS_VIEW);
        });
    }
}