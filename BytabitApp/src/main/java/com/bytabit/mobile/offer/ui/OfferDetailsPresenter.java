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

package com.bytabit.mobile.offer.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.DecimalTextFieldFormatter;
import com.bytabit.mobile.common.UiUtils;
import com.bytabit.mobile.offer.manager.OfferManager;
import com.bytabit.mobile.offer.model.Offer;
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
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.MathContext;

import static com.bytabit.mobile.offer.model.Offer.OfferType.BUY;
import static com.bytabit.mobile.offer.model.Offer.OfferType.SELL;

@Slf4j
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
    private Label typeLabel;

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
    private Button tradeBtcButton;

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


        JavaFxObservable.actionEventsOf(tradeBtcButton)
                .observeOn(Schedulers.io())
                .flatMapMaybe(ae -> {
                    try {
                        BigDecimal buyBtcAmount = new BigDecimal(buyBtcAmtTextField.textProperty().getValue());
                        return offerManager.createTrade(buyBtcAmount);
                    } catch (NumberFormatException nfe) {
                        throw new OfferDetailsPresenterException("Invalid number format for amount.");
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .doOnError(UiUtils::showErrorDialog)
                .retry()
                .subscribe(t -> {
                    log.debug("Create trade ", t);
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

        offerManager.getSelectedOffer()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .doOnNext(offer ->
                        walletManager.getProfilePubKeyBase58()
                                .subscribe(profilePubKey -> {
                                    if (profilePubKey.equals(offer.getMakerProfilePubKey())) {
                                        // my offer
                                        buyGridPane.setVisible(false);
                                        removeOfferButton.setVisible(true);
                                    } else {
                                        // not my offer
                                        buyGridPane.setVisible(true);
                                        removeOfferButton.setVisible(false);
                                    }
                                }))
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

    private void showOffer(Offer offer) {

        paymentMethodLabel.setText(offer.getPaymentMethod().displayName());
        typeLabel.setText(offer.getOfferType().toString());
        tradeBtcButton.setText(SELL.equals(offer.getOfferType()) ? BUY.toString() : SELL.toString());
        String currencyCode = offer.getCurrencyCode().toString();
        currencyLabel.setText(currencyCode);
        currencyAmtLabel.setText(currencyCode);
        minTradeAmtLabel.setText(offer.getMinAmount().toPlainString());
        minTradeAmtCurrencyLabel.setText(currencyCode);
        maxTradeAmtLabel.setText(offer.getMinAmount().toPlainString());
        maxTradeAmtCurrencyLabel.setText(currencyCode);
        priceLabel.setText(offer.getPrice().toPlainString());
        priceCurrencyLabel.setText(currencyCode);
    }
}
