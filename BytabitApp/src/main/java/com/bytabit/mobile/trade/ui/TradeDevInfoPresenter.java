package com.bytabit.mobile.trade.ui;

import com.bytabit.mobile.trade.manager.TradeManager;
import com.bytabit.mobile.trade.model.Trade;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class TradeDevInfoPresenter {

    private static Logger log = LoggerFactory.getLogger(TradeDevInfoPresenter.class);

    @Inject
    TradeManager tradeManager;

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

        log.debug("initialize trade debug info presenter");

        JavaFxObservable.changesOf(tradeDevInfoView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> setAppBar());

        tradeManager.getLastSelectedTrade().autoConnect()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::showTrade);
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Trade Debug Info");
    }

    private void showTrade(Trade trade) {
        tradeEscrowAddressLabel.textProperty().setValue(trade.getEscrowAddress());
        sellerEscrowPubKeyLabel.textProperty().setValue(trade.getSellerEscrowPubKey());
        sellerProfilePubKeyLabel.textProperty().setValue(trade.getSellerProfilePubKey());
        arbitratorProfilePubKeyLabel.textProperty().setValue(trade.getArbitratorProfilePubKey());
    }
}
