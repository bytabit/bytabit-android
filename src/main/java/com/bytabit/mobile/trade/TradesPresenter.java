package com.bytabit.mobile.trade;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.trade.model.Trade;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;

import javax.inject.Inject;

public class TradesPresenter {

    @Inject
    TradeManager tradeManager;

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

        //offersListView.itemsProperty().addAll(offerManager.read());
        tradesListView.itemsProperty().setValue(tradeManager.getTradesObservableList());
        tradesListView.selectedItemProperty().addListener((obs, oldValue, newValue) -> {
//            SellOffer viewOffer = tradeManager.getViewTrade();
//            viewOffer.setSellerEscrowPubKey(newValue.getSellerEscrowPubKey());
//            viewOffer.setSellerProfilePubKey(newValue.getSellerProfilePubKey());
//            viewOffer.setCurrencyCode(newValue.getCurrencyCode());
//            viewOffer.setPaymentMethod(newValue.getPaymentMethod());
//            viewOffer.setMinAmount(newValue.getMinAmount());
//            viewOffer.setMaxAmount(newValue.getMaxAmount());
//            viewOffer.setPrice(newValue.getPrice());
//            MobileApplication.getInstance().switchView(BytabitMobile.OFFER_DETAILS_VIEW);
        });
    }
}