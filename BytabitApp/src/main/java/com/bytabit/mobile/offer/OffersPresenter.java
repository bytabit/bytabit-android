package com.bytabit.mobile.offer;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.Event;
import com.bytabit.mobile.common.EventLogger;
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
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;

import javax.inject.Inject;

public class OffersPresenter {

    private final EventLogger eventLogger = EventLogger.of(OffersPresenter.class);

    @Inject
    OfferManager offerManager;

//    @Inject
//    ProfileManager profileManager;
//
//    @Inject
//    TradeManager tradeManager;
//
//    @Inject
//    WalletManager walletManager;

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

        Observable<PresenterEvent> viewShowingEvents = JavaFxObservable.changesOf(offersView.showingProperty())
                .map(showing -> showing.getNewVal() ? new ViewShowing() : new ViewNotShowing());

        Observable<PresenterEvent> addOfferButtonEvents = Observable.create(source ->
                addOfferButton.setOnAction(source::onNext))
                .map(actionEvent -> new AddButtonPressed());

        Observable<PresenterEvent> offerEvents = Observable.merge(viewShowingEvents,
                addOfferButtonEvents)
                .compose(eventLogger.logEvents()).share();

        // transform events to actions

        // handle events

        offerEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(OffersPresenter.ViewShowing.class)
                .subscribe(event -> {
                    setAppBar();
                    //clearForm();
                });

        offerEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(OffersPresenter.AddButtonPressed.class)
                .subscribe(event -> {
                    MobileApplication.getInstance().switchView(BytabitMobile.ADD_OFFER_VIEW);
                });

        // handle results

        //offersListView.setItems(offerManager.get());

//        addOfferButton.setOnAction((e) ->
//                MobileApplication.getInstance().switchView(BytabitMobile.ADD_OFFER_VIEW));

        offerManager.getResults().ofType(OfferManager.OfferUpdated.class)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(result -> {
                    SellOffer sellOffer = result.getOffer();
                    int index = offersListView.itemsProperty().indexOf(sellOffer);
                    if (index > -1) {
                        offersListView.itemsProperty().remove(index);
                    }
                    offersListView.itemsProperty().add(sellOffer);
                });

//        offersView.showingProperty().addListener((obs, oldValue, newValue) -> {
//            if (newValue) {
//                //LOG.debug("Offers view pending.");
//
//                AppBar appBar = MobileApplication.getInstance().getAppBar();
//                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
//                        MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
//                appBar.setTitleText("Offers");
//                appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
//                        System.out.println("Search")));
//
////                offerManager.observableOffers().observeOn(JavaFxScheduler.platform())
////                        .subscribe(ol -> offersListView.itemsProperty().setAll(ol));
//            }
//        });

//        offerManager.getResults().subscribeOn(Schedulers.io())
//                .observeOn(JavaFxScheduler.platform()).subscribe(so ->
//                //LOG.info(so.toString())
//        );

//        offerManager.get().observeOn(JavaFxScheduler.platform())
//                .subscribe(ol -> offersListView.itemsProperty().setAll(ol));

//        offerManager.observableOffers().observeOn(JavaFxScheduler.platform())
//                .subscribe(ol -> offerManager.get().setAll(ol));

//        offersListView.selectedItemProperty().addListener((obs, oldValue, selectedSellOffer) -> {
//            if (selectedSellOffer != null) {
//                String sellerEscrowPubKey = selectedSellOffer.getSellerEscrowPubKey();
//                offersListView.selectedItemProperty().setValue(null);
////                offerManager.setSelectedOffer(selectedSellOffer);
//                MobileApplication.getInstance().switchView(BytabitMobile.OFFER_DETAILS_VIEW);
////                if (tradeManager.activeSellerEscrowPubKey(sellerEscrowPubKey)) {
////                    // TODO go to active trade details view for this sell offer
////                    MobileApplication.getInstance().switchView(BytabitMobile.TRADE_VIEW);
////                } else {
////                    offerManager.getSelectedSellOfferProperty().setValue(selectedSellOffer);
////                    offerManager.getSellerEscrowPubKeyProperty().setValue(sellerEscrowPubKey);
////                    offerManager.getSellerProfilePubKeyProperty().setValue(selectedSellOffer.getSellerProfilePubKey());
////                    offerManager.getArbitratorProfilePubKeyProperty().setValue(selectedSellOffer.getArbitratorProfilePubKey());
////                    offerManager.getCurrencyCodeProperty().setValue(selectedSellOffer.getCurrencyCode());
////                    offerManager.getPaymentMethodProperty().setValue(selectedSellOffer.getPaymentMethod());
////                    offerManager.getMinAmountProperty().setValue(selectedSellOffer.getMinAmount());
////                    offerManager.getMaxAmountProperty().setValue(selectedSellOffer.getMaxAmount());
////                    offerManager.getPriceProperty().setValue(selectedSellOffer.getPrice());
////                    MobileApplication.getInstance().switchView(BytabitMobile.OFFER_DETAILS_VIEW);
////                }
//            }
//        });
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
        appBar.setTitleText("Offers");
        appBar.getActionItems().add(MaterialDesignIcon.SEARCH.button(e ->
                System.out.println("Search")));
    }

    // Event classes

    interface PresenterEvent extends Event {
    }

    private class ViewShowing implements PresenterEvent {
    }

    private class ViewNotShowing implements PresenterEvent {
    }

    private class OfferChanged implements PresenterEvent {
        private final SellOffer sellOffer;

        public OfferChanged(SellOffer sellOffer) {
            this.sellOffer = sellOffer;
        }

        public SellOffer getSellOffer() {
            return sellOffer;
        }
    }

    private class AddButtonPressed implements PresenterEvent {
    }
}