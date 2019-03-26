/*
 * Copyright 2019 Bytabit AB
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

package com.bytabit.mobile.help.ui;

import com.bytabit.mobile.common.UiUtils;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.BrowserService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;

@Slf4j
public class HelpPresenter {

    @FXML
    private View helpView;

    @FXML
    private Hyperlink telegramURL;

    public HelpPresenter() {

    }

    public void initialize() {

        // setup event observables
        JavaFxObservable.changesOf(helpView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> {
                    setAppBar();
                });

        JavaFxObservable.actionEventsOf(telegramURL)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .doOnError(UiUtils::showErrorDialog)
                .retry()
                .subscribe(offer -> {
                    openUrl("https://t.me/bytabit");
                });
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Help");
    }

    /**
     * check if there is a handler registered for dealing with this protocol.
     */
    private void openUrl(String url) {

        Services.get(BrowserService.class).ifPresent(bs ->
        {
            try {
                bs.launchExternalBrowser(url);
            } catch (IOException e) {
                log.error("IO Exception {}", e);
            } catch (URISyntaxException e) {
                log.error("URI Exception {}", e);
            }
        });
    }
}
