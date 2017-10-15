package com.bytabit.mobile.profile;

import com.bytabit.mobile.BytabitMobile;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class ProfilePresenter {

    private static Logger LOG = LoggerFactory.getLogger(ProfilePresenter.class);

    @Inject
    private ProfileManager profileManager;

    @FXML
    private View profileView;

    @FXML
    private TextField pubKeyTextField;

    @FXML
    private CheckBox arbitratorCheckbox;

    @FXML
    private TextField userNameTextField;

    @FXML
    private TextField phoneNumTextField;

    public void initialize() {

        LOG.debug("initialize profile presenter");

        if (profileManager.getPubKeyProperty().getValue() == null) {
            profileManager.createProfile();
        }

        pubKeyTextField.textProperty().bind(profileManager.getPubKeyProperty());

        arbitratorCheckbox.selectedProperty().bindBidirectional(profileManager.getIsArbitratorProperty());
        userNameTextField.textProperty().bindBidirectional(profileManager.getUserNameProperty());
        phoneNumTextField.textProperty().bindBidirectional(profileManager.getPhoneNumProperty());

        profileView.showingProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> {
                    profileManager.updateProfile();
                    MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER);
                }));
                appBar.setTitleText("Profile");
            }
        });

        //profileManager.readProfiles();
    }
}
