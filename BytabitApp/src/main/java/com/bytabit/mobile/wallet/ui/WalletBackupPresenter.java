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

package com.bytabit.mobile.wallet.ui;

import com.bytabit.mobile.wallet.manager.WalletManager;
import com.bytabit.mobile.wallet.model.TradeWalletInfo;
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
import javafx.scene.control.TextArea;

import javax.inject.Inject;

public class WalletBackupPresenter {

    @Inject
    WalletManager walletManager;

    @FXML
    private View walletBackupView;

    @FXML
    private TextArea profilePubKeyTextArea;

    @FXML
    private Button copyProfilePubKeyButton;

    @FXML
    private TextArea seedWordsTextArea;

    @FXML
    private Button copySeedWordsButton;

    @FXML
    private TextArea xprvTextArea;

    @FXML
    private Button copyXprvButton;

    @FXML
    private TextArea xpubTextArea;

    @FXML
    private Button copyXpubButton;

    public void initialize() {

        JavaFxObservable.changesOf(walletBackupView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(change -> setAppBar());

        walletManager.getTradeWalletInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::setInfo);
    }

    private void setInfo(TradeWalletInfo walletInfo) {
        profilePubKeyTextArea.setText(walletInfo.getProfilePubKey());
        seedWordsTextArea.setText(walletInfo.getSeedWords());
        xprvTextArea.setText(walletInfo.getXprvKey());
        xpubTextArea.setText(walletInfo.getXpubKey());
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(ae -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Wallet Backup");
    }
}
