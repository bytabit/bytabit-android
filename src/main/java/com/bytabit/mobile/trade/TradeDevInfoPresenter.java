package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.Trade;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class TradeDevInfoPresenter {

    private static Logger LOG = LoggerFactory.getLogger(TradeDevInfoPresenter.class);

    @Inject
    private TradeManager tradeManager;

    @FXML
    private View tradeDevInfoView;

    @FXML
    private Label tradeEscrowAddressLabel;

    @FXML
    private Label sellerEscrowPubKeyLabel;

    @FXML
    private Label sellerProfilePubKeyLabel;

    @FXML
    private Label arbitratorProfilePubKeyLabel;

    public void initialize() {

        LOG.debug("initialize trade debug info presenter");

        tradeDevInfoView.showingProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
                appBar.setTitleText("Trade Debug Info");

                Trade trade = tradeManager.getSelectedTrade();

                tradeEscrowAddressLabel.textProperty().setValue(trade.getEscrowAddress());
                sellerEscrowPubKeyLabel.textProperty().setValue(trade.getSellerEscrowPubKey());
                sellerProfilePubKeyLabel.textProperty().setValue(trade.getSellerProfilePubKey());
                arbitratorProfilePubKeyLabel.textProperty().setValue(trade.getArbitratorProfilePubKey());
            }
        });
    }
}
