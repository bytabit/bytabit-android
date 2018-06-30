package com.bytabit.mobile.offer.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.offer.manager.OfferManager;
import com.bytabit.mobile.offer.model.SellOffer;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.ListTile;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;

import javax.inject.Inject;

public class OffersPresenter {

    private final EventLogger eventLogger = EventLogger.of(OffersPresenter.class);

    @Inject
    OfferManager offerManager;

    @FXML
    View offersView;

    @FXML
    CharmListView<SellOffer, String> offersListView;

    FloatingActionButton addOfferButton = new FloatingActionButton();

    public void initialize() {

        // setup view components

        offersView.getLayers().add(addOfferButton.getLayer());

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

        // setup event observables

        JavaFxObservable.changesOf(offersView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> {
                    setAppBar();
                    clearSelection();
                });

        Observable.create(source ->
                addOfferButton.setOnAction(source::onNext))
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(a ->
                        MobileApplication.getInstance().switchView(BytabitMobile.ADD_OFFER_VIEW)
                );

        JavaFxObservable.changesOf(offersListView.selectedItemProperty())
                .map(Change::getNewVal)
                .subscribe(sellOffer -> {
                    MobileApplication.getInstance().switchView(BytabitMobile.OFFER_DETAILS_VIEW);
                    offerManager.setSelectedOffer(sellOffer);
                });

        Observable.concat(offerManager.getLoadedOffers(), offerManager.getUpdatedOffers())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(sellOffers ->
                        offersListView.itemsProperty().setAll(sellOffers)
                );

        offerManager.getCreatedOffer()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(sellOffer ->
                        offersListView.itemsProperty().add(sellOffer)
                );

        offerManager.getRemovedOffer()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(sellOffer ->
                        offersListView.itemsProperty().remove(sellOffer)
                );
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
        appBar.setTitleText("Offers");
        appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
                System.out.println("Search")));
    }

    private void clearSelection() {
        offersListView.selectedItemProperty().setValue(null);
    }
}