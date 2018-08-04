package com.bytabit.mobile.offer.ui;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.DecimalTextFieldFormatter;
import com.bytabit.mobile.offer.manager.OfferManager;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.trade.manager.TradeManager;
import com.bytabit.mobile.trade.model.BuyRequest;
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
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.MathContext;

public class OfferDetailsPresenter {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    OfferManager offerManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    TradeManager tradeManager;

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
    private Label sellerEscrowPubKeyLabel;

    @FXML
    private Label sellerProfilePubKeyLabel;

    @FXML
    private Label currencyLabel;

    @FXML
    private Label paymentMethodLabel;

    @FXML
    private Label arbitratorProfilePubKeyLabel;

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

    public OfferDetailsPresenter() {
    }

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
                    offerManager.deleteOffer(sellerEscrowPubKeyLabel.getText());
                    MobileApplication.getInstance().switchToPreviousView();
                });


        JavaFxObservable.actionEventsOf(buyBtcButton)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(sellOffer -> {
                    offerManager.createTrade(new BigDecimal(buyBtcAmtTextField.textProperty().getValue()));
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
                .observeOn(Schedulers.io())
                .subscribe(offer -> profileManager.loadOrCreateMyProfile()
                        .subscribeOn(Schedulers.io())
                        .observeOn(JavaFxScheduler.platform())
                        .subscribe(myProfile -> {
                            if (myProfile.getPubKey().equals(offer.getSellerProfilePubKey())) {
                                // my offer
                                buyGridPane.setVisible(false);
                                removeOfferButton.setVisible(true);
                            } else {
                                // not my offer
                                buyGridPane.setVisible(true);
                                removeOfferButton.setVisible(false);
                            }
                            showOffer(offer);
                        }));

//        Observable.zip(
//                offerDetailEvents.ofType(BuyButtonPressed.class),
//                profileManager.getResults().ofType(ProfileManager.ProfileLoaded.class),
//                walletManager.getWalletResults().ofType(WalletManager.EscrowPubKey.class),
//                walletManager.getWalletResults().ofType(WalletManager.TradeWalletDepositAddress.class),
//                offerManager.getResults().ofType(OfferManager.OfferSelected.class),
//                (a, p, e, d, s) -> {
//                    SellOffer sellOffer = s.getOffer();
//                    BuyRequest buyRequest = createBuyRequest(p.getProfile().getPubKey(), e.getPubKey(), d.getAddress().toBase58());
//                    String escrowAddress = walletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(), sellOffer.getSellerEscrowPubKey(), buyRequest.getBuyerEscrowPubKey());
//                    return tradeManager.new CreateTrade(escrowAddress, Trade.Role.BUYER, s.getOffer(), buyRequest);
//                });

    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Offer Details");
    }

    private void showOffer(SellOffer sellOffer) {

        sellerEscrowPubKeyLabel.setText(sellOffer.getSellerEscrowPubKey());
        sellerProfilePubKeyLabel.setText(sellOffer.getSellerProfilePubKey());
        arbitratorProfilePubKeyLabel.setText(sellOffer.getArbitratorProfilePubKey());

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

    private BuyRequest createBuyRequest(String buyerProfilePubKey, String buyerEscrowPubKey,
                                        String buyerPayoutAddress) {

        BigDecimal btcAmount = new BigDecimal(buyBtcAmtTextField.textProperty().getValue());
        return new BuyRequest(buyerEscrowPubKey, btcAmount, buyerProfilePubKey, buyerPayoutAddress);
    }
}
