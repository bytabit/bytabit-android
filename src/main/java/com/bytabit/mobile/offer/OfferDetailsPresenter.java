package com.bytabit.mobile.offer;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.TradeManager;
import com.bytabit.mobile.wallet.WalletManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;

public class OfferDetailsPresenter {

    private static Logger LOG = LoggerFactory.getLogger(AddOfferPresenter.class);

    @Inject
    OfferManager offerManager;

    @Inject
    TradeManager tradeManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    WalletManager tradeWalletManager;

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

    public void initialize() {

        LOG.debug("initialize offer details presenter");

        offerDetailsView.showingProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
                appBar.setTitleText("Offer Details");
            }
        });

        sellerEscrowPubKeyLabel.textProperty().bind(offerManager.getSellerEscrowPubKeyProperty());
        sellerProfilePubKeyLabel.textProperty().bind(offerManager.getSellerProfilePubKeyProperty());
        arbitratorProfilePubKeyLabel.textProperty().bind(offerManager.getArbitratorProfilePubKeyProperty());
        minTradeAmtLabel.textProperty().bind(Bindings.createStringBinding(() -> offerManager.getMinAmountProperty().get().toString(),
                offerManager.getMinAmountProperty()));
        maxTradeAmtLabel.textProperty().bind(Bindings.createStringBinding(() -> offerManager.getMaxAmountProperty().get().toString(),
                offerManager.getMaxAmountProperty()));
        priceLabel.textProperty().bind(Bindings.createStringBinding(() -> offerManager.getPriceProperty().get().toString(),
                offerManager.getPriceProperty()));

        StringBinding currencyCodeBinding = Bindings.createStringBinding(() -> offerManager.getCurrencyCodeProperty().get().toString(),
                offerManager.getMinAmountProperty(), offerManager.getCurrencyCodeProperty());

        currencyLabel.textProperty().bind(currencyCodeBinding);
        paymentMethodLabel.textProperty().bind(Bindings.createStringBinding(() ->
                        offerManager.getPaymentMethodProperty().get().displayName(),
                offerManager.getPaymentMethodProperty()));

        minTradeAmtCurrencyLabel.textProperty().bind(currencyCodeBinding);
        minTradeAmtCurrencyLabel.textProperty().bind(currencyCodeBinding);
        maxTradeAmtCurrencyLabel.textProperty().bind(currencyCodeBinding);
        priceCurrencyLabel.textProperty().bind(currencyCodeBinding);
        currencyAmtLabel.textProperty().bind(currencyCodeBinding);

        removeOfferButton.visibleProperty().bind(Bindings.createBooleanBinding(() ->
                        profileManager.getPubKeyProperty().getValue().equals(offerManager.getSellerProfilePubKeyProperty().getValue()),
                offerManager.getSellerProfilePubKeyProperty()));

        removeOfferButton.setOnAction(e -> {
            offerManager.deleteOffer();
            MobileApplication.getInstance().switchToPreviousView();
        });

        buyGridPane.visibleProperty().bind(Bindings.createBooleanBinding(() ->
                        !profileManager.getPubKeyProperty().getValue().equals(offerManager.getSellerProfilePubKeyProperty().getValue()),
                offerManager.getSellerProfilePubKeyProperty()));

        buyBtcAmtTextField.textProperty().bind(Bindings.createStringBinding(() -> {
            String curAmtStr = buyCurrencyAmtTextField.textProperty().getValue();
            if (curAmtStr != null && curAmtStr.length() > 0) {
                BigDecimal buyBtcAmt = new BigDecimal(curAmtStr).divide(offerManager.getPriceProperty().getValue(), 8, BigDecimal.ROUND_HALF_UP);
                offerManager.getBuyBtcAmountProperty().set(buyBtcAmt);
                return buyBtcAmt.toString();
            } else {
                return null;
            }
        }, offerManager.getPriceProperty(), buyCurrencyAmtTextField.textProperty()));

        buyBtcButton.setOnAction(e -> {
            String buyerEscrowPubKey = tradeWalletManager.getFreshBase58AuthPubKey();
            String buyerProfilePubKey = profileManager.getPubKeyProperty().get();
            String buyerPayoutAddress = tradeWalletManager.getDepositAddress().toBase58();

            SellOffer selectedSellOffer = offerManager.getSelectedSellOfferProperty().get();
            BigDecimal buyBtcAmount = offerManager.getBuyBtcAmountProperty().get();

            // TODO better input validation
            if (buyerEscrowPubKey != null && buyerProfilePubKey != null &&
                    buyBtcAmount != null && selectedSellOffer != null) {

                tradeManager.buyerCreateTrade(selectedSellOffer, buyBtcAmount, buyerEscrowPubKey,
                        buyerProfilePubKey, buyerPayoutAddress);
            }

            MobileApplication.getInstance().switchToPreviousView();
        });
    }
}
