package com.bytabit.mobile.trade;

import com.bytabit.mobile.offer.AddOfferPresenter;
import com.bytabit.mobile.offer.OfferManager;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.wallet.TradeWalletManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class TradeDetailsPresenter {

    private static Logger LOG = LoggerFactory.getLogger(AddOfferPresenter.class);

    @Inject
    OfferManager offerManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    TradeWalletManager tradeWalletManager;

    @FXML
    private View tradeDetailsView;

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
    private Label offerPubKeyLabel;

    @FXML
    private Label sellerPubKeyLabel;

    @FXML
    private Label currencyLabel;

    @FXML
    private Label paymentMethodLabel;

    public void initialize() {

        LOG.debug("initialize offer details presenter");

        tradeDetailsView.showingProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
                appBar.setTitleText("Offer Details");

                SellOffer viewOffer = offerManager.getViewSellOffer();
                offerPubKeyLabel.textProperty().setValue(viewOffer.getSellerEscrowPubKey());
                sellerPubKeyLabel.textProperty().setValue(viewOffer.getSellerProfilePubKey());
                currencyLabel.textProperty().setValue(viewOffer.getCurrencyCode().toString());
                paymentMethodLabel.textProperty().setValue(viewOffer.getPaymentMethod().displayName());
                minTradeAmtLabel.textProperty().setValue(viewOffer.getMinAmount().toString());
                maxTradeAmtLabel.textProperty().setValue(viewOffer.getMaxAmount().toString());
                priceLabel.textProperty().setValue(viewOffer.getPrice().toString());

                minTradeAmtCurrencyLabel.textProperty().setValue(viewOffer.getCurrencyCode().toString());
                maxTradeAmtCurrencyLabel.textProperty().setValue(viewOffer.getCurrencyCode().toString());
                priceCurrencyLabel.textProperty().setValue(viewOffer.getCurrencyCode().toString());

                String sellerPubKey = profileManager.profile().getPubKey();
                if (sellerPubKey != null && sellerPubKey.equals(viewOffer.getSellerProfilePubKey())) {
                    removeOfferButton.visibleProperty().setValue(true);
                } else {
                    removeOfferButton.visibleProperty().setValue(false);
                }
            }
        });
    }
}
