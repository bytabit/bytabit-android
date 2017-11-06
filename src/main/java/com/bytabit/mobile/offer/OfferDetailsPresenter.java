package com.bytabit.mobile.offer;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.TradeManager;
import com.bytabit.mobile.wallet.WalletManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Single;
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

    public OfferDetailsPresenter() {
    }

    public void initialize() {

        LOG.debug("initialize offer details presenter");

        offerDetailsView.showingProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
                appBar.setTitleText("Offer Details");

                sellerProfilePubKeyLabel.textProperty().setValue(offerManager.getSelectedOffer().getSellerProfilePubKey());
                sellerEscrowPubKeyLabel.textProperty().setValue(offerManager.getSelectedOffer().getSellerEscrowPubKey());
                arbitratorProfilePubKeyLabel.textProperty().setValue(offerManager.getSelectedOffer().getArbitratorProfilePubKey());
                currencyLabel.textProperty().setValue(offerManager.getSelectedOffer().getCurrencyCode().name());
                paymentMethodLabel.textProperty().setValue(offerManager.getSelectedOffer().getPaymentMethod().name());
                minTradeAmtLabel.textProperty().setValue(offerManager.getSelectedOffer().getMinAmount().toPlainString());
                minTradeAmtCurrencyLabel.textProperty().setValue(offerManager.getSelectedOffer().getCurrencyCode().name());
                maxTradeAmtLabel.textProperty().setValue(offerManager.getSelectedOffer().getMaxAmount().toPlainString());
                maxTradeAmtCurrencyLabel.textProperty().setValue(offerManager.getSelectedOffer().getCurrencyCode().name());
                priceLabel.textProperty().setValue(offerManager.getSelectedOffer().getPrice().toPlainString());
                priceCurrencyLabel.textProperty().setValue(offerManager.getSelectedOffer().getCurrencyCode().name());
                currencyAmtLabel.textProperty().setValue(offerManager.getSelectedOffer().getCurrencyCode().name());

                profileManager.retrieveMyProfile().observeOn(JavaFxScheduler.platform()).subscribe(p -> {
                    boolean isMyOffer = p.getPubKey().equals(offerManager.getSelectedOffer().getSellerProfilePubKey());
                    removeOfferButton.visibleProperty().set(isMyOffer);
                    buyGridPane.visibleProperty().set(!isMyOffer);
                });
            }
        });

        removeOfferButton.setOnAction(e -> {
            offerManager.deleteOffer(sellerEscrowPubKeyLabel.textProperty().get())
                    .doOnComplete(() -> offerManager.singleOffers().observeOn(JavaFxScheduler.platform())
                            .subscribe(ol -> offerManager.getOffers().setAll(ol))).subscribe();
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

            Single.zip(profileManager.retrieveMyProfile(), walletManager.getFreshBase58AuthPubKey(), (buyerProfile, buyerEscrowPubKey) -> {

                String buyerPayoutAddress = walletManager.getDepositAddress().toBase58();
                SellOffer selectedSellOffer = offerManager.getSelectedOffer();
                BigDecimal buyBtcAmount = new BigDecimal(buyBtcAmtTextField.textProperty().get());

                // TODO better input validation
                if (buyerProfile.getPubKey() != null && buyerEscrowPubKey != null && buyBtcAmount.compareTo(BigDecimal.ZERO) > 0 && selectedSellOffer != null) {

                    return tradeManager.buyerCreateTrade(selectedSellOffer, buyBtcAmount, buyerEscrowPubKey,
                            buyerProfile.getPubKey(), buyerPayoutAddress);
                } else {
                    return null;
                }
            }).flatMap(t -> t).observeOn(JavaFxScheduler.platform()).subscribe(trade -> tradeManager.getTrades().add(trade));

            MobileApplication.getInstance().switchToPreviousView();
        });
    }
}
