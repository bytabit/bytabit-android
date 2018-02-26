package com.bytabit.mobile.profile.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.Event;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.wallet.manager.WalletManager;
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
    ProfileManager profileManager;

    @Inject
    WalletManager walletManager;

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

        Observable<PresenterEvent> viewShowingEvents =
                JavaFxObservable.changesOf(profileView.showingProperty())
                        .map(showing -> {
                            if (showing.getNewVal()) {
                                return new ViewShowing();
                            } else
                                return new ViewNotShowing(createProfileFromUI());
                        });

        Observable<PresenterEvent> profileEvents = viewShowingEvents
                .compose(eventLogger.logEvents()).share();

        Observable<ProfileManager.ProfileAction> loadProfileActions = profileEvents
                .ofType(ViewShowing.class)
                .map(e -> profileManager.new LoadProfile());

        Observable<ProfileManager.ProfileAction> updateProfileActions = profileEvents
                .ofType(ViewNotShowing.class)
                .map(e -> profileManager.new UpdateProfile(e.getProfile()));

        Observable<WalletManager.WalletResult> walletResults =
                walletManager.getWalletResults();

        Observable<ProfileManager.ProfileAction> createProfileAction = walletResults
                .ofType(WalletManager.ProfilePubKey.class)
                .map(r -> profileManager.new CreateProfile(r.getPubKey()));

        Observable<ProfileManager.ProfileAction> profileActions = loadProfileActions
                .mergeWith(createProfileAction)
                .mergeWith(updateProfileActions);

        profileActions.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .compose(eventLogger.logEvents())
                .subscribe(profileManager.getActions());

        profileEvents.subscribeOn(Schedulers.io())
                .ofType(ViewShowing.class)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(e -> setAppBar());

        Observable<ProfileManager.ProfileResult> profileResults =
                profileManager.getResults();

        Observable<WalletManager.WalletAction> getProfilePubKeyAction = profileResults
                .ofType(ProfileManager.ProfileNotCreated.class)
                .map(r -> walletManager.new GetProfilePubKey());

        getProfilePubKeyAction.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .compose(eventLogger.logEvents())
                .subscribe(walletManager.getActions());

        profileResults.subscribeOn(Schedulers.io())
                .ofType(ProfileManager.ProfilePending.class)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(r -> profileView.setDisable(true));

        profileResults.subscribeOn(Schedulers.io())
                .ofType(ProfileManager.ProfileCreated.class)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(r -> {
                    profileView.setDisable(false);
                    setProfile(r.getProfile());
                });

        profileResults.subscribeOn(Schedulers.io())
                .ofType(ProfileManager.ProfileLoaded.class)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(r -> {
                    profileView.setDisable(false);
                    setProfile(r.getProfile());
                });

        profileResults.subscribeOn(Schedulers.io())
                .ofType(ProfileManager.ProfileUpdated.class)
                .observeOn(JavaFxScheduler.platform())
                .subscribe(r -> {
                    profileView.setDisable(false);
                    setProfile(r.getProfile());
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

    // Event classes

    private interface PresenterEvent extends Event {
    }

    private class ViewShowing implements PresenterEvent {
    }

    private class ViewNotShowing implements PresenterEvent {
        private final Profile profile;

        public ViewNotShowing(Profile profile) {
            this.profile = profile;
        }

        public Profile getProfile() {
            return profile;
        }
    }
}
