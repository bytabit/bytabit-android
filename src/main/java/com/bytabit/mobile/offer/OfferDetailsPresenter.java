package com.bytabit.mobile.offer;

import com.bytabit.mobile.offer.model.BuyRequest;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.TradeManager;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.EscrowWalletManager;
import com.bytabit.mobile.wallet.TradeWalletManager;
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
    TradeWalletManager tradeWalletManager;

    @Inject
    EscrowWalletManager escrowWalletManager;

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

    @FXML
    private Label btcPriceCurrencyLabel;

    public void initialize() {

        LOG.debug("initialize offer details presenter");

        offerDetailsView.showingProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
                appBar.setTitleText("Offer Details");
            }
        });

        SellOffer viewOffer = offerManager.getViewSellOffer();
        sellerEscrowPubKeyLabel.textProperty().bind(viewOffer.sellerEscrowPubKeyProperty());
        sellerProfilePubKeyLabel.textProperty().bind(viewOffer.sellerProfilePubKeyProperty());
        arbitratorProfilePubKeyLabel.textProperty().bind(viewOffer.arbitratorProfilePubKeyProperty());
        minTradeAmtLabel.textProperty().bind(Bindings.createStringBinding(() -> viewOffer.minAmountProperty().get().toString(),
                viewOffer.minAmountProperty()));
        maxTradeAmtLabel.textProperty().bind(Bindings.createStringBinding(() -> viewOffer.maxAmountProperty().get().toString(),
                viewOffer.maxAmountProperty()));
        priceLabel.textProperty().bind(Bindings.createStringBinding(() -> viewOffer.priceProperty().get().toString(),
                viewOffer.priceProperty()));

        StringBinding currencyCodeBinding = Bindings.createStringBinding(() -> viewOffer.currencyCodeProperty().get().toString(),
                viewOffer.minAmountProperty(), viewOffer.currencyCodeProperty());

        currencyLabel.textProperty().bind(currencyCodeBinding);
        paymentMethodLabel.textProperty().bind(Bindings.createStringBinding(() ->
                        viewOffer.paymentMethodProperty().get().displayName(),
                viewOffer.paymentMethodProperty()));

        minTradeAmtCurrencyLabel.textProperty().bind(currencyCodeBinding);
        minTradeAmtCurrencyLabel.textProperty().bind(currencyCodeBinding);
        maxTradeAmtCurrencyLabel.textProperty().bind(currencyCodeBinding);
        priceCurrencyLabel.textProperty().bind(currencyCodeBinding);
        currencyAmtLabel.textProperty().bind(currencyCodeBinding);

        removeOfferButton.visibleProperty().bind(Bindings.createBooleanBinding(() ->
                        profileManager.profile().getPubKey().equals(viewOffer.getSellerProfilePubKey()),
                viewOffer.sellerProfilePubKeyProperty()));

        removeOfferButton.setOnAction(e -> {
            offerManager.deleteOffer();
            MobileApplication.getInstance().switchToPreviousView();
        });

        buyGridPane.visibleProperty().bind(Bindings.createBooleanBinding(() ->
                        !profileManager.profile().getPubKey().equals(viewOffer.getSellerProfilePubKey()),
                viewOffer.sellerProfilePubKeyProperty()));

        buyBtcAmtTextField.textProperty().bind(Bindings.createStringBinding(() -> {
            String curAmtStr = buyCurrencyAmtTextField.textProperty().getValue();
            if (curAmtStr != null && curAmtStr.length() > 0) {
                BigDecimal buyBtcAmt = new BigDecimal(curAmtStr).divide(viewOffer.priceProperty().get(), 8, BigDecimal.ROUND_HALF_UP);
                offerManager.getBuyBtcAmount().set(buyBtcAmt);
                return buyBtcAmt.toString();
            } else {
                return null;
            }
        }, viewOffer.priceProperty(), buyCurrencyAmtTextField.textProperty()));

        buyBtcButton.setOnAction(e -> {
            String buyerEscrowPubKey = tradeWalletManager.getFreshBase58PubKey();
            String buyerProfilePubKey = profileManager.profile().getPubKey();
            String buyerPayoutAddress = tradeWalletManager.getDepositAddress().toBase58();
            BuyRequest createdBuyRequest = tradeManager.createBuyRequest(viewOffer,
                    offerManager.getBuyBtcAmount().get(), buyerEscrowPubKey,
                    buyerProfilePubKey, buyerPayoutAddress);
            Trade createdTrade = tradeManager.createTrade(viewOffer, createdBuyRequest);
            escrowWalletManager.watchTradeEscrowAddress(createdTrade.getEscrowAddress());
        });
    }
}
