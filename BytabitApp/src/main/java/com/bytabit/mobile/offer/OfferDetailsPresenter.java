package com.bytabit.mobile.offer;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.trade.TradeManager;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.beans.binding.Bindings;
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

    private Profile myProfile;

    private SellOffer selectedOffer;

    public OfferDetailsPresenter() {
    }

    public void initialize() {

        LOG.debug("initialize offer details presenter");

        offerManager.getSelectedOffer()
                .observeOn(JavaFxScheduler.platform())
                .subscribe(so -> {
                    selectedOffer = so;
                    sellerProfilePubKeyLabel.textProperty().setValue(selectedOffer.getSellerProfilePubKey());
                    sellerEscrowPubKeyLabel.textProperty().setValue(selectedOffer.getSellerEscrowPubKey());
                    arbitratorProfilePubKeyLabel.textProperty().setValue(selectedOffer.getArbitratorProfilePubKey());
                    currencyLabel.textProperty().setValue(selectedOffer.getCurrencyCode().name());
                    paymentMethodLabel.textProperty().setValue(selectedOffer.getPaymentMethod().name());
                    minTradeAmtLabel.textProperty().setValue(selectedOffer.getMinAmount().toPlainString());
                    minTradeAmtCurrencyLabel.textProperty().setValue(selectedOffer.getCurrencyCode().name());
                    maxTradeAmtLabel.textProperty().setValue(selectedOffer.getMaxAmount().toPlainString());
                    maxTradeAmtCurrencyLabel.textProperty().setValue(selectedOffer.getCurrencyCode().name());
                    priceLabel.textProperty().setValue(selectedOffer.getPrice().toPlainString());
                    priceCurrencyLabel.textProperty().setValue(selectedOffer.getCurrencyCode().name());
                    currencyAmtLabel.textProperty().setValue(selectedOffer.getCurrencyCode().name());

//                    profileManager.loadMyProfile().observeOn(JavaFxScheduler.platform()).subscribe(p -> {
//                        boolean isMyOffer = p.getPubKey().equals(selectedOffer.getSellerProfilePubKey());
//                        removeOfferButton.visibleProperty().set(isMyOffer);
//                        buyGridPane.visibleProperty().set(!isMyOffer);
//                    });
                });

        offerDetailsView.showingProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
                appBar.setTitleText("Offer Details");
            }
        });

        removeOfferButton.setOnAction(e -> {
            offerManager.deleteOffer(sellerEscrowPubKeyLabel.textProperty().get());
            //.doOnComplete(() -> offerManager.singleOffers().observeOn(JavaFxScheduler.platform())
            //.subscribe(ol -> offerManager.getOffers().setAll(ol))).subscribe();
            MobileApplication.getInstance().switchToPreviousView();
        });

        buyBtcAmtTextField.textProperty().bind(Bindings.createStringBinding(() -> {
            String curAmtStr = buyCurrencyAmtTextField.textProperty().getValue();
            String curPriceStr = priceLabel.textProperty().getValue();
            if (curAmtStr != null && curAmtStr.length() > 0 && curAmtStr.matches("^\\d+(\\.\\d+)?$")) {
                BigDecimal buyBtcAmt = new BigDecimal(curAmtStr).divide(new BigDecimal(curPriceStr), 8, BigDecimal.ROUND_HALF_UP);
                //offerManager.getBuyBtcAmountProperty().set(buyBtcAmt);
                return buyBtcAmt.toString();
            } else {
                return null;
            }
        }, priceLabel.textProperty(), buyCurrencyAmtTextField.textProperty()));

        buyBtcButton.setOnAction(e -> {
//            Single.zip(profileManager.loadMyProfile(), walletManager.getFreshBase58AuthPubKey(), walletManager.getDepositAddress(),
//                    (buyerProfile, buyerEscrowPubKey, depositAddress) -> {
//                        String buyerPayoutAddress = depositAddress.toBase58();
//                        BigDecimal buyBtcAmount = new BigDecimal(buyBtcAmtTextField.textProperty().get());
//
//                        // TODO input validation
//                        BuyRequest buyRequest = new BuyRequest(buyerEscrowPubKey, buyBtcAmount, buyerProfile.getPubKey(), buyerPayoutAddress);
//                        return tradeManager.buyerCreateTrade(getSelectedOffer(), buyRequest);
//                    })
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(JavaFxScheduler.platform())
//                    .doOnSuccess(created -> {
//                        LOG.info("Created trade.");
//                        MobileApplication.getInstance().switchToPreviousView();
//                    })
//                    .doOnError(exception -> {
//                        LOG.error("Couldn't create trade.", exception);
//                    }).subscribe();
        });
    }

    private SellOffer getSelectedOffer() {
        return selectedOffer;
    }
}
