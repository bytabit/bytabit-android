package com.bytabit.mobile.profile;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.profile.PaymentsResult.MyProfileResult;
import com.bytabit.mobile.profile.action.MyProfileAction;
import com.bytabit.mobile.profile.event.MyProfileEvent;
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

//    private final EventLogger eventLogger = EventLogger.of(LOG);

    public void initialize() {

        LOG.debug("initialize profile presenter");

        Observable<MyProfileEvent> viewShowingEvents =
                JavaFxObservable.changesOf(profileView.showingProperty())
                        .map(showing -> {
                            if (showing.getNewVal()) {
                                return MyProfileEvent.viewShowing();
                            } else
                                return MyProfileEvent.viewNotShowing(createProfileFromUI());
                        });

        Observable<MyProfileEvent> myProfileEvents = viewShowingEvents.publish().refCount();

        Observable<MyProfileAction> myProfileActions = myProfileEvents.map(event -> {
            switch (event.getType()) {
                case VIEW_SHOWING:
                    return MyProfileAction.load();
                case VIEW_NOT_SHOWING:
                    return MyProfileAction.update(event.getData());
                default:
                    throw new RuntimeException("Unexpected MyProfileEvent.Type");
            }
        });

        Observable<MyProfileResult> myProfileResults = myProfileActions
                .compose(profileManager.myProfileActionTransformer());

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
                            setProfile(result.getData());
                            break;
                        case ERROR:
                            LOG.error(result.getError().getMessage());
                            break;
                    }
                });
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> {
            MobileApplication.getInstance().showLayer(BytabitMobile.MENU_LAYER);
        }));
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
