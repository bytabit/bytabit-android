package com.bytabit.ft.profile;

import com.bytabit.ft.FiatTraderMobile;
import com.bytabit.ft.wallet.TradeWalletManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import org.bitcoinj.core.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

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
    private CheckBox arbitratorCheckbox;

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

        // pubkey init and focus handler
        if (!profileManager.getPubKey().isPresent()) {
            Address profileKeyAddress = tradeWalletManager.getNewProfileKeyAddress();
            profileManager.setPubKey(profileKeyAddress.toBase58());
        }

        profileManager.getPubKey().ifPresent(pk -> pubKeyTextField.textProperty().setValue(pk));

        // isArbitrator init and focus handler
        profileManager.isArbitrator().ifPresent(a -> arbitratorCheckbox.setSelected(a));

        arbitratorCheckbox.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                profileManager.setIsArbitrator(arbitratorCheckbox.isSelected());
            }
        }));

        // name init and focus handler
        profileManager.getName().ifPresent(n -> nameTextField.textProperty().setValue(n));

        nameTextField.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                profileManager.setName(nameTextField.getText());
            }
        }));

        // phone num init and focus handler
        profileManager.getPhoneNum().ifPresent(pn -> phoneNumTextField.textProperty().setValue(pn));

        phoneNumTextField.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                profileManager.setPhoneNum(phoneNumTextField.getText());
            }
        }));

    }
}
