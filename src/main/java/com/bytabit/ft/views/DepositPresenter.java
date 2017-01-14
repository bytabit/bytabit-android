package com.bytabit.ft.views;

import com.bytabit.ft.FiatTraderMobile;
import com.gluonhq.charm.glisten.animation.BounceInRightTransition;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;

public class DepositPresenter {

    @FXML
    private View deposit;

    public void initialize() {
        deposit.setShowTransitionFactory(BounceInRightTransition::new);

        deposit.getLayers().add(new FloatingActionButton(MaterialDesignIcon.INFO.text,
                e -> System.out.println("Info")).getLayer());

        deposit.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(FiatTraderMobile.MENU_LAYER)));
                appBar.setTitleText("Secondary");
                appBar.getActionItems().add(MaterialDesignIcon.FAVORITE.button(e ->
                        System.out.println("Favorite")));
            }
        });
    }
}
