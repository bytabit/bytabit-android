package com.bytabit.mobile.offer.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.DecimalTextFieldFormatter;
import com.bytabit.mobile.offer.manager.OfferManager;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.ShareService;
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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.MathContext;

public class OfferDetailsPresenter {

    @Inject
    OfferManager offerManager;

    @Inject
    WalletManager walletManager;

    @FXML
    private View offerDetailsView;

    @FXML
    private Button removeOfferButton;

    @FXML
    private Label minTradeAmtLabel;

    @FXML
    private Label minTradeAmtCurrencyLabel;

    @FXML
    private Label maxTradeAmtLabel;

    @FXML
    private Label maxTradeAmtCurrencyLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Label priceCurrencyLabel;

    @FXML
    private Label currencyLabel;

    @FXML
    private Label paymentMethodLabel;

    @FXML
    private GridPane buyGridPane;

    @FXML
    private Button buyBtcButton;

    @FXML
    private TextField buyCurrencyAmtTextField;

    @FXML
    private Label currencyAmtLabel;

    @FXML
    private TextField buyBtcAmtTextField;

    public void initialize() {

        // setup view components

        buyCurrencyAmtTextField.setTextFormatter(new DecimalTextFieldFormatter());
        buyBtcAmtTextField.setTextFormatter(new DecimalTextFieldFormatter());

        // setup event observables

        JavaFxObservable.changesOf(offerDetailsView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> setAppBar());

        JavaFxObservable.actionEventsOf(removeOfferButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(actionEvent -> {
                    offerManager.deleteOffer();
                    MobileApplication.getInstance().switchToPreviousView();
                });


        JavaFxObservable.actionEventsOf(buyBtcButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(action -> {
                    offerManager.createTrade(new BigDecimal(buyBtcAmtTextField.textProperty().getValue())).subscribe();
                    MobileApplication.getInstance().switchView(BytabitMobile.TRADE_VIEW);
                });

        JavaFxObservable.changesOf(buyCurrencyAmtTextField.textProperty())
                .map(change -> change.getNewVal() == null || change.getNewVal().isEmpty() ? "0.00" : change.getNewVal())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .map(BigDecimal::new)
                .subscribe(newCurrencyAmount -> {
                    BigDecimal priceAmount = new BigDecimal(priceLabel.getText());
                    BigDecimal btcAmount = newCurrencyAmount.divide(priceAmount, MathContext.DECIMAL32)
                            .setScale(8, BigDecimal.ROUND_UP);
                    buyBtcAmtTextField.setText(btcAmount.toPlainString());
                });

        offerManager.getLastSelectedOffer().autoConnect()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .zipWith(walletManager.getProfilePubKeyBase58().toObservable(), (offer, profilePubKey) -> {
                    if (profilePubKey.equals(offer.getSellerProfilePubKey())) {
                        // my offer
                        buyGridPane.setVisible(false);
                        removeOfferButton.setVisible(true);
                    } else {
                        // not my offer
                        buyGridPane.setVisible(true);
                        removeOfferButton.setVisible(false);
                    }
                    return offer;
                })
                .subscribe(this::showOffer);

    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Offer Details");
        appBar.getActionItems().add(MaterialDesignIcon.BUG_REPORT.button(e ->
                offerManager.getSelectedOfferAsJson().subscribe(this::debugOffer)));
    }

    private void debugOffer(String offerJson) {
        if (Platform.isDesktop()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(offerJson);
            clipboard.setContent(content);
        } else {
            ShareService shareService = Services.get(ShareService.class).orElseThrow(() -> new RuntimeException("ShareService not available."));
            shareService.share(offerJson);
        }
    }

    private void showOffer(SellOffer sellOffer) {

        paymentMethodLabel.setText(sellOffer.getPaymentMethod().displayName());

        String currencyCode = sellOffer.getCurrencyCode().toString();
        currencyLabel.setText(currencyCode);
        currencyAmtLabel.setText(currencyCode);
        minTradeAmtLabel.setText(sellOffer.getMinAmount().toPlainString());
        minTradeAmtCurrencyLabel.setText(currencyCode);
        maxTradeAmtLabel.setText(sellOffer.getMinAmount().toPlainString());
        maxTradeAmtCurrencyLabel.setText(currencyCode);
        priceLabel.setText(sellOffer.getPrice().toPlainString());
        priceCurrencyLabel.setText(currencyCode);
    }
}
