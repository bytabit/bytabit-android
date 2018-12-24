/*
 * Copyright 2018 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import javafx.scene.control.TextField;

import javax.inject.Inject;

public class ProfilePresenter {

    @Inject
    ProfileManager profileManager;

    @FXML
    private View profileView;

    @FXML
    private TextField userNameTextField;

    @FXML
    private TextField phoneNumTextField;

    @FXML
    private Button saveProfileButton;

    public void initialize() {

        JavaFxObservable.actionEventsOf(saveProfileButton)
                .map(a -> profileManager.storeMyProfile(getProfile()))
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(a -> MobileApplication.getInstance().switchToPreviousView());

        JavaFxObservable.changesOf(profileView.showingProperty())
                .filter(Change::getNewVal)
                .map(e -> profileManager.retrieveMyProfile())
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
                .userName(userNameTextField.getText())
                .phoneNum(phoneNumTextField.getText()).build();
    }

    private void setProfile(Profile profile) {
        userNameTextField.setText(profile.getUserName());
        phoneNumTextField.setText(profile.getPhoneNum());
    }
}
