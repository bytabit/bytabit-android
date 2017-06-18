package com.bytabit.mobile.profile;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.wallet.WalletManager;
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
    private WalletManager tradeWalletManager;

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

        //tradeWalletManager.startWallet();
        tradeWalletManager.tradeWalletRunningProperty().addListener((obj, oldVal, isRunning) -> {
            if (isRunning) {
                LOG.debug("Wallet Running");
                // pubkey init
                if (profileManager.profile().getPubKey() == null) {
                    String profilePubKey = tradeWalletManager.getFreshBase58PubKey();
                    profileManager.createProfile(profilePubKey);
                    LOG.debug("Profile PubKey Initialized");
                }
            }
        });

        profileView.showingProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
                appBar.setTitleText("Profile");
            }
        });

        Profile profile = profileManager.profile();
        pubKeyTextField.textProperty().bind(profile.pubKeyProperty());
        arbitratorCheckbox.selectedProperty().bindBidirectional(profile.isArbitratorProperty());
        nameTextField.textProperty().bindBidirectional(profile.nameProperty());
        phoneNumTextField.textProperty().bindBidirectional(profile.phoneNumProperty());

        //profileManager.readProfiles();
    }
}
