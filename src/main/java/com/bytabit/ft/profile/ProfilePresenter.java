package com.bytabit.ft.profile;

import com.bytabit.ft.FiatTraderMobile;
import com.bytabit.ft.wallet.TradeWalletManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.TextField;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import org.bitcoinj.core.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class ProfilePresenter {

    private static Logger LOG = LoggerFactory.getLogger(ProfilePresenter.class);

    @Inject
    private TradeWalletManager tradeWalletManager;

    @Inject
    private ProfileManager profileManager;

    @FXML
    private View profileView;

    @FXML
    private TextField pubKeyTextField;

    @FXML
    private TextField nameTextField;

    @FXML
    private TextField phoneNumTextField;

    public void initialize() {

        LOG.debug("initialize profile presenter");

        profileView.showingProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(FiatTraderMobile.MENU_LAYER)));
                appBar.setTitleText("Profile");
            }
        });

        if (!profileManager.getPubKey().isPresent()) {
            Address profileKeyAddress = tradeWalletManager.getNewProfileKeyAddress();
            profileManager.setPubKey(profileKeyAddress.toBase58());
        }
        profileManager.getPubKey().ifPresent(pk -> pubKeyTextField.textProperty().setValue(pk));

        profileManager.getName().ifPresent(n -> nameTextField.textProperty().setValue(n));

        nameTextField.textProperty().addListener(((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                profileManager.setName(newValue);
            }
        }));

        profileManager.getPhoneNum().ifPresent(pn -> phoneNumTextField.textProperty().setValue(pn));

        phoneNumTextField.textProperty().addListener(((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                profileManager.setPhoneNum(newValue);
            }
        }));
    }
}
