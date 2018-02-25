package com.bytabit.mobile.offer;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.trade.TradeManager;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.*;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class OffersPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(OffersPresenter.class);

    @Inject
    OfferManager offerManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    TradeManager tradeManager;

    @Inject
    WalletManager walletManager;

    @FXML
    View offersView;

    @FXML
    CharmListView<SellOffer, String> offersListView;

    FloatingActionButton addOfferButton = new FloatingActionButton();

    public void initialize() {

        tradeManager.initialize();

        offersListView.setCellFactory((view) -> new CharmListCell<SellOffer>() {
            @Override
            public void updateItem(SellOffer o, boolean empty) {
                super.updateItem(o, empty);
                if (o != null && !empty) {
                    ListTile tile = new ListTile();
                    String amount = String.format("%s %s per BTC via %s", o.getPrice().toPlainString(), o.getCurrencyCode().toString(), o.getPaymentMethod().displayName());
                    String details = String.format("%s to %s %s",
                            o.getMinAmount(), o.getMaxAmount(), o.getCurrencyCode());
                    tile.textProperty().addAll(amount, details);
                    setText(null);
                    setGraphic(tile);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });

        //offersListView.setItems(offerManager.getOffers());

        offersView.getLayers().add(addOfferButton.getLayer());

        addOfferButton.setOnAction((e) ->
                MobileApplication.getInstance().switchView(BytabitMobile.ADD_OFFER_VIEW));

        offersView.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                LOG.debug("Offers view pending.");

                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
                appBar.setTitleText("Offers");
                appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
                        System.out.println("Search")));

//                offerManager.observableOffers().observeOn(JavaFxScheduler.platform())
//                        .subscribe(ol -> offersListView.itemsProperty().setAll(ol));
            }
        });

        offersView.setOnShown(e -> {
            if (e.getEventType().equals(LifecycleEvent.SHOWN)) {
                LOG.debug("Offers view shown.");
//                walletManager.start();
            }
        });

        offerManager.getOffers().observeOn(JavaFxScheduler.platform())
                .subscribe(ol -> offersListView.itemsProperty().setAll(ol));

//        offerManager.observableOffers().observeOn(JavaFxScheduler.platform())
//                .subscribe(ol -> offerManager.getOffers().setAll(ol));

        offersListView.selectedItemProperty().addListener((obs, oldValue, selectedSellOffer) -> {
            if (selectedSellOffer != null) {
                String sellerEscrowPubKey = selectedSellOffer.getSellerEscrowPubKey();
                offersListView.selectedItemProperty().setValue(null);
                offerManager.setSelectedOffer(selectedSellOffer);
                MobileApplication.getInstance().switchView(BytabitMobile.OFFER_DETAILS_VIEW);
//                if (tradeManager.activeSellerEscrowPubKey(sellerEscrowPubKey)) {
//                    // TODO go to active trade details view for this sell offer
//                    MobileApplication.getInstance().switchView(BytabitMobile.TRADE_VIEW);
//                } else {
//                    offerManager.getSelectedSellOfferProperty().setValue(selectedSellOffer);
//                    offerManager.getSellerEscrowPubKeyProperty().setValue(sellerEscrowPubKey);
//                    offerManager.getSellerProfilePubKeyProperty().setValue(selectedSellOffer.getSellerProfilePubKey());
//                    offerManager.getArbitratorProfilePubKeyProperty().setValue(selectedSellOffer.getArbitratorProfilePubKey());
//                    offerManager.getCurrencyCodeProperty().setValue(selectedSellOffer.getCurrencyCode());
//                    offerManager.getPaymentMethodProperty().setValue(selectedSellOffer.getPaymentMethod());
//                    offerManager.getMinAmountProperty().setValue(selectedSellOffer.getMinAmount());
//                    offerManager.getMaxAmountProperty().setValue(selectedSellOffer.getMaxAmount());
//                    offerManager.getPriceProperty().setValue(selectedSellOffer.getPrice());
//                    MobileApplication.getInstance().switchView(BytabitMobile.OFFER_DETAILS_VIEW);
//                }
            }
        });
    }

}