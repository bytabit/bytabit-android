package com.bytabit.mobile.nav;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.config.AppConfig;
import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.LifecycleService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.NavigationDrawer;
import com.gluonhq.charm.glisten.control.NavigationDrawer.Item;
import com.gluonhq.charm.glisten.control.NavigationDrawer.ViewItem;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import static com.bytabit.mobile.BytabitMobile.*;

public class NavDrawer {

    private final NavigationDrawer drawer;

    public NavDrawer() {

        String config = AppConfig.getConfigName().equals("default") ? "" : ", " + AppConfig.getConfigName();
        NavigationDrawer.Header header = new NavigationDrawer.Header("Bytabit Mobile",
                String.format("%s (%s)", AppConfig.getVersion(), AppConfig.getBtcNetwork() + config),
                new ImageView(new Image(NavDrawer.class.getResourceAsStream("/logo42.png"))));

        final Item tradesItem = new ViewItem("Trades", MaterialDesignIcon.SWAP_VERTICAL_CIRCLE.graphic(), TRADE_VIEW);
        final Item offersItem = new ViewItem("Offers", MaterialDesignIcon.SHOP.graphic(), OFFERS_VIEW);
        final Item walletItem = new ViewItem("Wallet", MaterialDesignIcon.ACCOUNT_BALANCE_WALLET.graphic(), WALLET_VIEW);
        final Item paymentDetailsItem = new ViewItem("Payment Details", MaterialDesignIcon.ACCOUNT_BALANCE.graphic(), PAYMENT_VIEW);
        final Item profileItem = new ViewItem("Profile", MaterialDesignIcon.ACCOUNT_CIRCLE.graphic(), PROFILE_VIEW);
        final Item aboutItem = new ViewItem("Help", MaterialDesignIcon.HELP.graphic(), HELP_VIEW);
        aboutItem.setDisable(true);

        ObservableList<Node> items = FXCollections.observableArrayList();
        items.addAll(tradesItem, offersItem, walletItem, paymentDetailsItem, profileItem, aboutItem);
        this.drawer = new NavigationDrawer(header, items);

        if (Platform.isDesktop()) {
            final Item quitItem = new Item("Quit", MaterialDesignIcon.EXIT_TO_APP.graphic());
            quitItem.selectedProperty().addListener((obs, ov, nv) -> {
                if (nv) {
                    BytabitMobile.getNavEventsSubject().onNext(NavEvent.QUIT);
                    Services.get(LifecycleService.class).ifPresent(LifecycleService::shutdown);
                }
            });
            drawer.getItems().add(quitItem);
        }

        drawer.addEventHandler(NavigationDrawer.ITEM_SELECTED,
                e -> MobileApplication.getInstance().hideLayer(MENU_LAYER));

        MobileApplication.getInstance().viewProperty().addListener((obs, oldView, newView) -> updateItem(newView.getId()));
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
