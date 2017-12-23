package com.bytabit.mobile.wallet;

import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class WalletBackupPresenter {

    private static Logger LOG = LoggerFactory.getLogger(WalletBackupPresenter.class);

    @Inject
    private WalletManager walletManager;

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
        LOG.debug("initialize wallet backup presenter");

        walletBackupView.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));

                appBar.setTitleText("Wallet Backup");
            }

//            seedWordsTextArea.setText(walletManager.getSeedWords());
//            xprvTextArea.setText(walletManager.getXprvKey());
//            xpubTextArea.setText(walletManager.getXpubKey());
        });

    }
}
