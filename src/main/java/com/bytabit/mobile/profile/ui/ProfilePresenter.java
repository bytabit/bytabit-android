package com.bytabit.mobile.profile.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.profile.manager.ProfileAction;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.manager.ProfileResult;
import com.bytabit.mobile.profile.model.Profile;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import javax.inject.Inject;

public class ProfilePresenter {

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

    private final EventLogger eventLogger = EventLogger.of(ProfilePresenter.class);

    public void initialize() {

        Observable<ProfileEvent> viewShowingEvents =
                JavaFxObservable.changesOf(profileView.showingProperty())
                        .map(showing -> {
                            if (showing.getNewVal()) {
                                return ProfileEvent.viewShowing();
                            } else
                                return ProfileEvent.viewNotShowing(createProfileFromUI());
                        });

        Observable<ProfileEvent> myProfileEvents = viewShowingEvents
                .compose(eventLogger.logEvents()).share();

        Observable<ProfileAction> myProfileActions = myProfileEvents.map(event -> {
            switch (event.getType()) {
                case VIEW_SHOWING:
                    return ProfileAction.load();
                case VIEW_NOT_SHOWING:
                    return ProfileAction.update(event.getProfile());
                default:
                    throw new RuntimeException(String.format("Unexpected ProfileEvent.Type: %s", event.getType()));
            }
        });

        myProfileActions.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(profileManager.getMyProfileActions());

        Observable<ProfileResult> myProfileResults = profileManager.getMyProfileResults();

        myProfileEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(event -> {
                    switch (event.getType()) {
                        case VIEW_SHOWING:
                            setAppBar();
                            break;
                        case VIEW_NOT_SHOWING:
                            break;
                    }
                });

        myProfileResults.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(result -> {
                    switch (result.getType()) {
                        case PENDING:
                            profileView.setDisable(true);
                            break;
                        case CREATED:
                        case LOADED:
                        case UPDATED:
                            profileView.setDisable(false);
                            setProfile(result.getProfile());
                            break;
                        case ERROR:
                            break;
                    }
                });
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER)));
        appBar.setTitleText("Profile");
    }

    private Profile createProfileFromUI() {
        return Profile.builder()
                .pubKey(pubKeyTextField.getText())
                .isArbitrator(arbitratorCheckbox.isSelected())
                .userName(userNameTextField.getText())
                .phoneNum(phoneNumTextField.getText()).build();
    }

    private void setProfile(Profile profile) {
        pubKeyTextField.setText(profile.getPubKey());
        if (profile.getIsArbitrator() != null) {
            arbitratorCheckbox.setSelected(profile.getIsArbitrator());
        }
        userNameTextField.setText(profile.getUserName());
        phoneNumTextField.setText(profile.getPhoneNum());
    }
}