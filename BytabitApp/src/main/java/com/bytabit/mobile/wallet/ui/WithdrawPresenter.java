package com.bytabit.mobile.wallet.ui;

import com.bytabit.mobile.common.DecimalTextFieldFormatter;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
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
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.math.BigDecimal;

@Slf4j
public class WithdrawPresenter {

    @FXML
    private View withdrawView;

    @FXML
    private TextField withdrawAddressField;

    @FXML
    private TextField availableAmountField;

    @FXML
    private TextField withdrawAmountField;

    @FXML
    private Button withdrawButton;

    @Inject
    WalletManager walletManager;

    public void initialize() {

        withdrawAmountField.setTextFormatter(new DecimalTextFieldFormatter());

        JavaFxObservable.changesOf(withdrawView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(showing -> setAppBar());

        JavaFxObservable.changesOf(withdrawView.showingProperty())
                .filter(Change::getNewVal)
                .flatMap(showing -> walletManager.getTradeUpdatedWalletTx().autoConnect())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(this::showAvailableAmount);

        JavaFxObservable.actionEventsOf(withdrawButton)
                .flatMapSingle(actionEvent -> {
                    String address = withdrawAddressField.getText();
                    BigDecimal amount = new BigDecimal(withdrawAmountField.getText());
                    return walletManager.withdrawFromTradeWallet(address, amount);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(tx -> MobileApplication.getInstance().switchToPreviousView());
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(ae -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Withdraw");
        appBar.getActionItems().add(MaterialDesignIcon.CAMERA_ALT.button(e ->
                log.debug("Scan BTC Address...")));
    }

    private void showAvailableAmount(TransactionWithAmt tx) {
        availableAmountField.setText(tx.getWalletBalance().toFriendlyString());
    }
}
