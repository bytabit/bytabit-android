package com.bytabit.mobile.offer;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.common.DecimalTextFieldFormatter;
import com.bytabit.mobile.common.Event;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.trade.TradeManager;
import com.bytabit.mobile.trade.model.BuyRequest;
import com.bytabit.mobile.trade.model.Trade;
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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.MathContext;

public class OfferDetailsPresenter {

    private final EventLogger eventLogger = EventLogger.of(OfferDetailsPresenter.class);

    @Inject
    OfferManager offerManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    TradeManager tradeManager;

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

        // setup view components

        buyCurrencyAmtTextField.setTextFormatter(new DecimalTextFieldFormatter());
        buyBtcAmtTextField.setTextFormatter(new DecimalTextFieldFormatter());

        // setup event observables

        Observable<PresenterEvent> viewShowingEvents = JavaFxObservable.changesOf(offerDetailsView.showingProperty())
                .map(showing -> showing.getNewVal() ? new ViewShowing() : new ViewNotShowing());

        Observable<PresenterEvent> removeOfferButtonPressedEvents = JavaFxObservable.actionEventsOf(removeOfferButton)
                .map(actionEvent -> new RemoveOfferButtonPressed());

        Observable<PresenterEvent> buyButtonPressedEvents = JavaFxObservable.actionEventsOf(buyBtcButton)
                .map(actionEvent -> new BuyButtonPressed());

        Observable<PresenterEvent> currencyAmountChangedEvents = JavaFxObservable.changesOf(buyCurrencyAmtTextField.textProperty())
                .map(change -> change.getNewVal() == null || change.getNewVal().isEmpty() ? "0.00" : change.getNewVal())
                .map(amt -> new CurrencyAmountChanged(new BigDecimal(amt)));

        Observable<PresenterEvent> offerDetailEvents = Observable.merge(viewShowingEvents,
                removeOfferButtonPressedEvents, buyButtonPressedEvents, currencyAmountChangedEvents)
                .compose(eventLogger.logEvents()).share();


        // transform events to actions

        Observable<ProfileManager.LoadProfile> loadProfileActions = offerDetailEvents
                .ofType(ViewShowing.class)
                .map(o -> profileManager.new LoadProfile());

        loadProfileActions
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .compose(eventLogger.logEvents())
                .subscribe(a -> profileManager.getActions().onNext(a));

        Observable<OfferManager.RemoveOffer> removeOfferActions = offerDetailEvents
                .ofType(RemoveOfferButtonPressed.class)
                .map(o -> offerManager.new RemoveOffer(sellerEscrowPubKeyLabel.getText()));

        removeOfferActions
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .compose(eventLogger.logEvents())
                .subscribe(a -> offerManager.getActions().onNext(a));

        Observable<WalletManager.GetEscrowPubKey> getEscrowPubKeyActions = offerDetailEvents
                .ofType(BuyButtonPressed.class)
                .map(e -> walletManager.new GetEscrowPubKey());

        Observable<WalletManager.GetTradeWalletDepositAddress> getDepositAddressActions = offerDetailEvents
                .ofType(BuyButtonPressed.class)
                .map(e -> walletManager.new GetTradeWalletDepositAddress());

        Observable.merge(getEscrowPubKeyActions, getDepositAddressActions)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .compose(eventLogger.logEvents())
                .subscribe(a -> walletManager.getActions().onNext(a));

        Observable<TradeManager.CreateTrade> createTradeActions = Observable.zip(
                offerDetailEvents.ofType(BuyButtonPressed.class),
                profileManager.getResults().ofType(ProfileManager.ProfileLoaded.class),
                walletManager.getWalletResults().ofType(WalletManager.EscrowPubKey.class),
                walletManager.getWalletResults().ofType(WalletManager.TradeWalletDepositAddress.class),
                offerManager.getResults().ofType(OfferManager.OfferSelected.class),
                (a, p, e, d, s) -> {
                    SellOffer sellOffer = s.getOffer();
                    BuyRequest buyRequest = createBuyRequest(p.getProfile().getPubKey(), e.getPubKey(), d.getAddress().toBase58());
                    String escrowAddress = walletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(), sellOffer.getSellerEscrowPubKey(), buyRequest.getBuyerEscrowPubKey());
                    return tradeManager.new CreateTrade(escrowAddress, Trade.Role.BUYER, s.getOffer(), buyRequest);
                });

        createTradeActions
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .compose(eventLogger.logEvents())
                .subscribe(tradeManager.getActions());

        // handle events

        offerDetailEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(ViewShowing.class)
                .subscribe(event -> {
                    setAppBar();
                });

        offerDetailEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(CurrencyAmountChanged.class)
                .subscribe(event -> {
                    BigDecimal priceAmount = new BigDecimal(priceLabel.getText());
                    BigDecimal btcAmount = event.newAmount.divide(priceAmount, MathContext.DECIMAL32)
                            .setScale(8, BigDecimal.ROUND_UP);
                    buyBtcAmtTextField.setText(btcAmount.toPlainString());
                });

        offerDetailEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(BuyButtonPressed.class)
                .subscribe(event -> {
                    MobileApplication.getInstance().switchView(BytabitMobile.TRADE_VIEW);
                });

        // handle results

//        offerManager.getResults().ofType(OfferManager.OfferSelected.class)
//                .compose(eventLogger.logResults())
//                .map(OfferManager.OfferSelected::getOffer)
//                .subscribeOn(Schedulers.io())
//                .observeOn(JavaFxScheduler.platform())
//                .subscribe(this::showOffer);

        Observable<ProfileManager.ProfileLoaded> profileLoadedResults = profileManager.getResults()
                .ofType(ProfileManager.ProfileLoaded.class).share();

        Observable<OfferManager.OfferSelected> offerSelectedResults = offerManager.getResults()
                .ofType(OfferManager.OfferSelected.class).share();

        Observable<PresenterEvent> showingEvents = Observable.zip(
                offerSelectedResults, profileLoadedResults, (offer, profile) -> {
                    if (offer.getOffer().getSellerProfilePubKey().equals(profile.getProfile().getPubKey())) {
                        return new MyOfferSelected(offer.getOffer());
                    } else {
                        return new OtherOfferSelected(offer.getOffer());
                    }
                })
                .compose(eventLogger.logEvents())
                .share();

        showingEvents.ofType(MyOfferSelected.class)
                .map(MyOfferSelected::getSellOffer)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(offer -> {
                    buyGridPane.setVisible(false);
                    removeOfferButton.setVisible(true);
                    showOffer(offer);
                });

        showingEvents.ofType(OtherOfferSelected.class)
                .map(OtherOfferSelected::getSellOffer)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(offer -> {
                    buyGridPane.setVisible(true);
                    removeOfferButton.setVisible(false);
                    showOffer(offer);
                });

        offerManager.getResults().ofType(OfferManager.OfferRemoved.class)
                .observeOn(JavaFxScheduler.platform())
                .map(OfferManager.OfferRemoved::getSellerEscrowPubKey)
                .subscribe(sellerEscrowPubKey -> {
                    MobileApplication.getInstance().switchToPreviousView();
                });

//        offerManager.getSelectedOffer()
//                .observeOn(JavaFxScheduler.platform())
//                .subscribe(so -> {
//                    selectedOffer = so;
//                    sellerProfilePubKeyLabel.textProperty().setValue(selectedOffer.getSellerProfilePubKey());
//                    sellerEscrowPubKeyLabel.textProperty().setValue(selectedOffer.getSellerEscrowPubKey());
//                    arbitratorProfilePubKeyLabel.textProperty().setValue(selectedOffer.getArbitratorProfilePubKey());
//                    currencyLabel.textProperty().setValue(selectedOffer.getCurrencyCode().name());
//                    paymentMethodLabel.textProperty().setValue(selectedOffer.getPaymentMethod().name());
//                    minTradeAmtLabel.textProperty().setValue(selectedOffer.getMinAmount().toPlainString());
//                    minTradeAmtCurrencyLabel.textProperty().setValue(selectedOffer.getCurrencyCode().name());
//                    maxTradeAmtLabel.textProperty().setValue(selectedOffer.getMaxAmount().toPlainString());
//                    maxTradeAmtCurrencyLabel.textProperty().setValue(selectedOffer.getCurrencyCode().name());
//                    priceLabel.textProperty().setValue(selectedOffer.getPrice().toPlainString());
//                    priceCurrencyLabel.textProperty().setValue(selectedOffer.getCurrencyCode().name());
//                    currencyAmtLabel.textProperty().setValue(selectedOffer.getCurrencyCode().name());
//
////                    profileManager.loadMyProfile().observeOn(JavaFxScheduler.platform()).subscribe(p -> {
////                        boolean isMyOffer = p.getPubKey().equals(selectedOffer.getSellerProfilePubKey());
////                        removeOfferButton.visibleProperty().set(isMyOffer);
////                        buyGridPane.visibleProperty().set(!isMyOffer);
////                    });
//                });

//        offerDetailsView.showingProperty().addListener((observable, oldValue, newValue) -> {
//
//            if (newValue) {
//                AppBar appBar = MobileApplication.getInstance().getAppBar();
//                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
//                appBar.setTitleText("Offer Details");
//            }
//        });
//
//        removeOfferButton.setOnAction(e -> {
//            offerManager.deleteOffer(sellerEscrowPubKeyLabel.textProperty().get());
//            //.doOnComplete(() -> offerManager.singleOffers().observeOn(JavaFxScheduler.platform())
//            //.subscribe(ol -> offerManager.getAll().setAll(ol))).subscribe();
//            MobileApplication.getInstance().switchToPreviousView();
//        });
//
//        buyBtcAmtTextField.textProperty().bind(Bindings.createStringBinding(() -> {
//            String curAmtStr = buyCurrencyAmtTextField.textProperty().getValue();
//            String curPriceStr = priceLabel.textProperty().getValue();
//            if (curAmtStr != null && curAmtStr.length() > 0 && curAmtStr.matches("^\\d+(\\.\\d+)?$")) {
//                BigDecimal buyBtcAmt = new BigDecimal(curAmtStr).divide(new BigDecimal(curPriceStr), 8, BigDecimal.ROUND_HALF_UP);
//                //offerManager.getBuyBtcAmountProperty().set(buyBtcAmt);
//                return buyBtcAmt.toString();
//            } else {
//                return null;
//            }
//        }, priceLabel.textProperty(), buyCurrencyAmtTextField.textProperty()));

//        buyBtcButton.setOnAction(e -> {
//            Single.zip(profileManager.loadMyProfile(), walletManager.getFreshBase58AuthPubKey(), walletManager.getDepositAddress(),
//                    (buyerProfile, buyerEscrowPubKey, depositAddress) -> {
//                        String buyerPayoutAddress = depositAddress.toBase58();
//                        BigDecimal buyBtcAmount = new BigDecimal(buyBtcAmtTextField.textProperty().getAll());
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
//        });
//    }

//    private SellOffer getSelectedOffer() {
//        return selectedOffer;
//    }

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

    // Event classes

    interface PresenterEvent extends Event {
    }

    private class ViewShowing implements PresenterEvent {
    }

    private class ViewNotShowing implements PresenterEvent {
    }

    private class RemoveOfferButtonPressed implements PresenterEvent {
    }

    private class BuyButtonPressed implements PresenterEvent {
    }

    private class MyOfferSelected implements PresenterEvent {

        private final SellOffer sellOffer;

        public MyOfferSelected(SellOffer sellOffer) {
            this.sellOffer = sellOffer;
        }

        public SellOffer getSellOffer() {
            return sellOffer;
        }
    }

    private class OtherOfferSelected implements PresenterEvent {

        private final SellOffer sellOffer;

        public OtherOfferSelected(SellOffer sellOffer) {
            this.sellOffer = sellOffer;
        }

        public SellOffer getSellOffer() {
            return sellOffer;
        }
    }

    private class CurrencyAmountChanged implements PresenterEvent {
        private final BigDecimal newAmount;

        public CurrencyAmountChanged(BigDecimal newAmount) {
            this.newAmount = newAmount;
        }

        public BigDecimal getNewAmount() {
            return newAmount;
        }
    }
}
