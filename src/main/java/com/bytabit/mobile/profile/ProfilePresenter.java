package com.bytabit.mobile.profile;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.wallet.TradeWalletManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
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
                        MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
                appBar.setTitleText("Profile");
            }
        });

        // pubkey init and focus handler
        if (!profileManager.readPubKey().isPresent()) {
            String profilePubKey = tradeWalletManager.getFreshBase58PubKey();
            profileManager.updatePubKey(profilePubKey);
        }

        profileManager.readPubKey().ifPresent(pk -> pubKeyTextField.textProperty().setValue(pk));

        // readIsArbitrator init and focus handler
        profileManager.readIsArbitrator().ifPresent(a -> arbitratorCheckbox.setSelected(a));

        arbitratorCheckbox.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                profileManager.updateIsArbitrator(arbitratorCheckbox.isSelected());
            }
        }));

        // name init and focus handler
        profileManager.readName().ifPresent(n -> nameTextField.textProperty().setValue(n));

        nameTextField.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                profileManager.updateName(nameTextField.getText());
            }
        }));

        // phone num init and focus handler
        profileManager.readPhoneNum().ifPresent(pn -> phoneNumTextField.textProperty().setValue(pn));

        phoneNumTextField.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                profileManager.updatePhoneNum(phoneNumTextField.getText());
            }
        }));

    }
}
