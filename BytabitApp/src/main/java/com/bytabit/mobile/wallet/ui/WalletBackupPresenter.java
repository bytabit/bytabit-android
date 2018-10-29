package com.bytabit.mobile.wallet.ui;

import com.bytabit.mobile.wallet.manager.WalletManager;
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

    private void setInfo(WalletManager.TradeWalletInfo walletInfo) {
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
