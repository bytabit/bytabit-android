package com.bytabit.mobile.offer;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.offer.model.Offer;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;

import javax.inject.Inject;

public class OffersPresenter {

    @Inject
    OfferManager offerManager;

    @FXML
    private View offersView;

    @FXML
    private CharmListView<Offer, String> offersListView;

    private FloatingActionButton addOfferButton = new FloatingActionButton();

    public void initialize() {
        offersListView.setCellFactory((view) -> new CharmListCell<Offer>() {
            @Override
            public void updateItem(Offer o, boolean empty) {
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
        //offersListView.setComparator((s1, s2) -> -1 * Integer.compare(s2.getDepth(), s1.getDepth()));

        offersView.getLayers().add(addOfferButton.getLayer());
        addOfferButton.setOnAction((e) ->
                MobileApplication.getInstance().switchView(BytabitMobile.ADD_OFFER_VIEW));

        offersView.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {

                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
                appBar.setTitleText("Offers");
                appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
                        System.out.println("Search")));
            }

        });

        //offersListView.itemsProperty().addAll(offerManager.readOffers());
        offersListView.itemsProperty().setValue(offerManager.getOffersObservableList());
        offerManager.readOffers();
    }

}