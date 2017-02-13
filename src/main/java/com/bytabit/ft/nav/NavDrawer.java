package com.bytabit.ft.nav;

import com.bytabit.ft.FiatTraderMobile;
import com.bytabit.ft.config.AppConfig;
import com.bytabit.ft.nav.evt.QuitEvent;
import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.LifecycleService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.NavigationDrawer;
import com.gluonhq.charm.glisten.control.NavigationDrawer.Item;
import com.gluonhq.charm.glisten.control.NavigationDrawer.ViewItem;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import rx.Observable;

import static com.bytabit.ft.FiatTraderMobile.*;

public class NavDrawer {

    private final NavigationDrawer drawer;

    public NavDrawer() {

        this.drawer = new NavigationDrawer();

        NavigationDrawer.Header header = new NavigationDrawer.Header("Fiat Trader Mobile",
                String.format("%s (%s)", AppConfig.getVersion(), AppConfig.getBtcNetwork()),
                new ImageView(new Image(NavDrawer.class.getResourceAsStream("/logo42.png"))));
        drawer.setHeader(header);

        final Item offersItem = new ViewItem("Offers", MaterialDesignIcon.SHOP.graphic(), OFFER_VIEW);
        offersItem.setDisable(true);
        final Item tradesItem = new ViewItem("Trades", MaterialDesignIcon.SWAP_VERTICAL_CIRCLE.graphic(), TRADE_VIEW);
        tradesItem.setDisable(true);
        final Item walletItem = new ViewItem("Wallet", MaterialDesignIcon.ACCOUNT_BALANCE_WALLET.graphic(), WALLET_VIEW);
        final Item paymentDetailsItem = new ViewItem("Payment Details", MaterialDesignIcon.ACCOUNT_BALANCE.graphic(), PAYMENT_VIEW);
        final Item profileItem = new ViewItem("Profile", MaterialDesignIcon.ACCOUNT_CIRCLE.graphic(), PROFILE_VIEW);
        final Item contractsItem = new ViewItem("Contracts", MaterialDesignIcon.DESCRIPTION.graphic(), CONTRACT_VIEW);
        contractsItem.setDisable(true);

        drawer.getItems().addAll(offersItem, tradesItem, walletItem, paymentDetailsItem, profileItem, contractsItem);

        if (Platform.isDesktop()) {
            final Item quitItem = new Item("Quit", MaterialDesignIcon.EXIT_TO_APP.graphic());
            quitItem.selectedProperty().addListener((obs, ov, nv) -> {
                if (nv) {
                    FiatTraderMobile.getNavEventsComposite().add(Observable.just(new QuitEvent()));
                    Services.get(LifecycleService.class).ifPresent(LifecycleService::shutdown);
                }
            });
            drawer.getItems().add(quitItem);
        }

        drawer.addEventHandler(NavigationDrawer.ITEM_SELECTED,
                e -> MobileApplication.getInstance().hideLayer(MENU_LAYER));

        MobileApplication.getInstance().viewProperty().addListener((obs, oldView, newView) -> updateItem(newView.getName()));
        updateItem(HOME_VIEW);
    }

    private void updateItem(String nameView) {
        for (Node item : drawer.getItems()) {
            if (item instanceof ViewItem && ((ViewItem) item).getViewName().equals(nameView)) {
                drawer.setSelectedItem(item);
                break;
            }
        }
    }

    public NavigationDrawer getDrawer() {
        return drawer;
    }
}
