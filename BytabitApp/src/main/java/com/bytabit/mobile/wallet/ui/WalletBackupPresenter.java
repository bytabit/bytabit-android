package com.bytabit.mobile.wallet.ui;

import com.bytabit.mobile.common.Event;
import com.bytabit.mobile.common.EventLogger;
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
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import javax.inject.Inject;

public class WalletBackupPresenter {

    private final EventLogger eventLogger = EventLogger.of(WalletBackupPresenter.class);

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

        Observable<WalletBackupPresenter.PresenterEvent> viewShowingEvents =
                JavaFxObservable.changesOf(walletBackupView.showingProperty())
                        .map(showing -> {
                            if (showing.getNewVal()) {
                                return new ViewShowing();
                            } else
                                return new ViewNotShowing();
                        });

        Observable<PresenterEvent> walletBackupEvents = viewShowingEvents
                .compose(eventLogger.logEvents()).share();

        walletBackupEvents.ofType(ViewShowing.class).subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(e -> {
                    AppBar appBar = MobileApplication.getInstance().getAppBar();
                    appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(ae -> MobileApplication.getInstance().switchToPreviousView()));
                    appBar.setTitleText("Wallet Backup");
                });

        Observable<WalletManager.GetTradeWalletInfo> getTradeWalletInfoActions = walletBackupEvents
                .ofType(ViewShowing.class)
                .map(e -> walletManager.new GetTradeWalletInfo());

        getTradeWalletInfoActions
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .compose(eventLogger.logEvents())
                .subscribe(walletManager.getActions());

        Observable<WalletManager.WalletResult> walletResults = walletManager.getWalletResults();

        walletResults.ofType(WalletManager.TradeWalletInfo.class)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(r -> {
                    seedWordsTextArea.setText(r.getSeedWords());
                    xprvTextArea.setText(r.getXprvKey());
                    xpubTextArea.setText(r.getXpubKey());
                });
    }

    // Event classes

    private interface PresenterEvent extends Event {
    }

    private class ViewShowing implements PresenterEvent {
    }

    private class ViewNotShowing implements PresenterEvent {
    }
}
