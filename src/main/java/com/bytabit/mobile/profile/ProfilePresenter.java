package com.bytabit.mobile.profile;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.profile.model.Profile;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Single;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
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

        profileView.showingProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> {
                    MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER);
                }));
                appBar.setTitleText("Profile");
                refreshProfile(profileManager.retrieveMyProfile());
            } else {
                Profile updatedProfile = Profile.builder()
                        .pubKey(pubKeyTextField.getText())
                        .isArbitrator(arbitratorCheckbox.isSelected())
                        .userName(userNameTextField.getText())
                        .phoneNum(phoneNumTextField.getText())
                        .build();
                profileManager.storeMyProfile(updatedProfile).subscribe();
            }
        });
    }

    private void refreshProfile(Single<Profile> profileObservable) {
        profileObservable.observeOn(JavaFxScheduler.platform()).subscribe(p -> {
            pubKeyTextField.setText(p.getPubKey());
            arbitratorCheckbox.setSelected(p.getIsArbitrator());
            userNameTextField.setText(p.getUserName());
            phoneNumTextField.setText(p.getPhoneNum());
        });
    }
}
