package com.bytabit.mobile.profile.ui;

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
import javafx.scene.control.Button;
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

    @FXML
    private Button saveProfileButton;

    public void initialize() {

        JavaFxObservable.actionEventsOf(saveProfileButton)
                .doOnNext(a -> profileManager.updateMyProfile(getProfile()))
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(a -> MobileApplication.getInstance().switchToPreviousView());

        JavaFxObservable.changesOf(profileView.showingProperty())
                .filter(Change::getNewVal)
                .flatMap(e -> profileManager.loadOrCreateMyProfile().toObservable())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(profile -> {
                    setAppBar();
                    setProfile(profile);
                });

        profileManager.getUpdatedProfile()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::setProfile);
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Profile");
    }

    private Profile getProfile() {
        return Profile.builder()
                .pubKey(pubKeyTextField.getText())
                .isArbitrator(arbitratorCheckbox.isSelected())
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
