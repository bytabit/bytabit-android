package com.bytabit.mobile.profile.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.Profile;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import javax.inject.Inject;

public class ProfilePresenter {

    @Inject
    ProfileManager profileManager;

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

    private final EventLogger eventLogger = EventLogger.of(ProfilePresenter.class);

    public void initialize() {

        JavaFxObservable.changesOf(profileView.showingProperty())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .filter(c -> !c.getNewVal())
                .subscribe(c -> profileManager.updateMyProfile(getProfile()));

        JavaFxObservable.changesOf(profileView.showingProperty())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .filter(Change::getNewVal)
                .subscribe(c -> {
                    setAppBar();
                    profileManager.loadMyProfile().subscribe(this::setProfile);
                });

        profileManager.getUpdatedProfile()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::setProfile);
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
        appBar.setTitleText("Profile");
    }

    private Profile getProfile() {
        return Profile.builder()
                .pubKey(pubKeyTextField.getText())
                .arbitrator(arbitratorCheckbox.isSelected())
                .userName(userNameTextField.getText())
                .phoneNum(phoneNumTextField.getText()).build();
    }

    private void setProfile(Profile profile) {
        pubKeyTextField.setText(profile.getPubKey());
        arbitratorCheckbox.setSelected(profile.isArbitrator());
        userNameTextField.setText(profile.getUserName());
        phoneNumTextField.setText(profile.getPhoneNum());
    }
}
