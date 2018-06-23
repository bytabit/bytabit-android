package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.DecimalTextFieldFormatter;
import com.bytabit.mobile.common.Event;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.common.StringBigDecimalConverter;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class AddOfferPresenter {

    private final EventLogger eventLogger = EventLogger.of(AddOfferPresenter.class);

    @Inject
    OfferManager offerManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    WalletManager walletManager;

    @FXML
    private View addOfferView;

    @FXML
    private ChoiceBox<CurrencyCode> currencyChoiceBox;

    @FXML
    private ChoiceBox<PaymentMethod> paymentMethodChoiceBox;

    @FXML
    private ChoiceBox<Profile> arbitratorChoiceBox;

    @FXML
    private TextField btcPriceTextField;

    @FXML
    private Label btcPriceCurrencyLabel;

    @FXML
    private Button addOfferButton;

    @FXML
    private TextField minTradeAmtTextField;

    @FXML
    private Label minTradeAmtCurrencyLabel;

    @FXML
    private TextField maxTradeAmtTextField;

    @FXML
    private Label maxTradeAmtCurrencyLabel;

    final private Set<PaymentDetails> paymentDetails = new HashSet<>();

    final private Set<Profile> arbitrators = new HashSet<>();

    public void initialize() {

        // setup view components

        StringConverter<BigDecimal> bigDecConverter = new StringBigDecimalConverter();

        paymentMethodChoiceBox.setConverter(new PaymentMethodStringConverter());

        arbitratorChoiceBox.setConverter(new ProfileStringConverter());

        minTradeAmtTextField.setTextFormatter(new DecimalTextFieldFormatter());
        maxTradeAmtTextField.setTextFormatter(new DecimalTextFieldFormatter());
        btcPriceTextField.setTextFormatter(new DecimalTextFieldFormatter());

        // setup event observables

        Observable<PresenterEvent> viewShowingEvents = JavaFxObservable.changesOf(addOfferView.showingProperty())
                .map(showing -> showing.getNewVal() ? new ViewShowing() : new ViewNotShowing());

        Observable<PresenterEvent> addOfferButtonEvents = Observable.create(source ->
                addOfferButton.setOnAction(source::onNext))
                .map(actionEvent -> new AddButtonPressed());

        Observable<CurrencyCodeSelected> currencyCodeSelectedEvents = JavaFxObservable.changesOf(currencyChoiceBox.getSelectionModel().selectedItemProperty())
                .map(Change::getNewVal)
                .map(CurrencyCodeSelected::new);

        Observable<PaymentMethodSelected> paymentMethodSelectedEvents = JavaFxObservable.changesOf(paymentMethodChoiceBox.getSelectionModel().selectedItemProperty())
                .map(Change::getNewVal)
                .map(PaymentMethodSelected::new);

        Observable<PresenterEvent> addOfferEvents = Observable.merge(viewShowingEvents,
                addOfferButtonEvents, currencyCodeSelectedEvents, paymentMethodSelectedEvents)
                .compose(eventLogger.logEvents())
                .share();

        // transform events to actions

//        Observable<ProfileManager.LoadPaymentDetails> loadPaymentDetailsActions = addOfferEvents
//                .ofType(ViewShowing.class)
//                .map(e -> profileManager.new LoadPaymentDetails());
//
//        Observable<ProfileManager.LoadArbitratorProfiles> loadArbitratorProfilesActions = addOfferEvents
//                .ofType(ViewShowing.class)
//                .map(e -> profileManager.new LoadArbitratorProfiles());
//
//        Observable<ProfileManager.LoadProfile> loadProfileActions = addOfferEvents
//                .ofType(AddButtonPressed.class)
//                .map(e -> profileManager.new LoadProfile());
//
//        Observable<WalletManager.GetEscrowPubKey> getEscrowPubKeyActions = addOfferEvents
//                .ofType(AddButtonPressed.class)
//                .map(e -> walletManager.new GetEscrowPubKey());
//
//        Observable<OfferManager.CreateSellOffer> createOfferActions = Observable.zip(
//                addOfferEvents.ofType(AddButtonPressed.class),
//                profileManager.getResults().ofType(ProfileManager.ProfileLoaded.class),
//                walletManager.getWalletResults().ofType(WalletManager.EscrowPubKey.class),
//                (a, p, e) -> createOffer(p.getProfile().getPubKey(), e.getPubKey()))
//                .map(e -> offerManager.new CreateSellOffer(e));
//
//        createOfferActions
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .compose(eventLogger.logEvents())
//                .subscribe(offerManager.getActions());
//
//        Observable.merge(loadPaymentDetailsActions, loadArbitratorProfilesActions, loadProfileActions)
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .compose(eventLogger.logEvents())
//                .subscribe(profileManager.getActions());
//
//        getEscrowPubKeyActions
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .compose(eventLogger.logEvents())
//                .subscribe(walletManager.getActions());

        // handle events

        addOfferEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(ViewShowing.class)
                .subscribe(event -> {
                    setAppBar();
                    clearForm();
                });

        addOfferEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(AddButtonPressed.class)
                .subscribe(event -> {
                    MobileApplication.getInstance().switchToPreviousView();
                });

        addOfferEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(CurrencyCodeSelected.class)
                .flatMap(cc -> Observable.fromIterable(paymentDetails)
                        .filter(p -> p.getCurrencyCode().equals(cc.getCurrencyCode()))
                        .map(PaymentDetails::getPaymentMethod).sorted()
                        .toList().toObservable())
                .subscribe(pm -> {
                    paymentMethodChoiceBox.itemsProperty().getValue().setAll(pm);
                    paymentMethodChoiceBox.getSelectionModel().selectFirst();
                });

        addOfferEvents.subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .ofType(CurrencyCodeSelected.class)
                .map(CurrencyCodeSelected::getCurrencyCode)
                .map(CurrencyCode::toString)
                .subscribe(cc -> {
                    minTradeAmtCurrencyLabel.setText(cc);
                    maxTradeAmtCurrencyLabel.setText(cc);
                    btcPriceCurrencyLabel.setText(cc);
                });

        // handle results

//        profileManager.getResults().ofType(ProfileManager.PaymentDetailsLoaded.class)
//                .subscribeOn(Schedulers.io())
//                .observeOn(JavaFxScheduler.platform())
//                .subscribe(e -> {
//                    paymentDetails.add(e.getPaymentDetails());
//                    CurrencyCode currencyCode = e.getPaymentDetails().getCurrencyCode();
//                    if (currencyChoiceBox.itemsProperty().getValue().indexOf(currencyCode) == -1) {
//                        currencyChoiceBox.itemsProperty().getValue().add(currencyCode);
//                    }
//                    if (currencyChoiceBox.selectionModelProperty().getValue().isEmpty()) {
//                        currencyChoiceBox.selectionModelProperty().getValue().selectFirst();
//                    }
//                });
//
//        profileManager.getResults().ofType(ProfileManager.ArbitratorProfileLoaded.class)
//                .subscribeOn(Schedulers.io())
//                .observeOn(JavaFxScheduler.platform())
//                .subscribe(e -> {
//                    arbitrators.add(e.getProfile());
//                    Profile profile = e.getProfile();
//                    if (arbitratorChoiceBox.itemsProperty().getValue().indexOf(profile) == -1) {
//                        arbitratorChoiceBox.itemsProperty().getValue().add(profile);
//                    }
//                    if (arbitratorChoiceBox.selectionModelProperty().getValue().isEmpty()) {
//                        arbitratorChoiceBox.selectionModelProperty().getValue().selectFirst();
//                    }
//                });
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Add Sell Offer");
    }

    private void clearForm() {
        currencyChoiceBox.getItems().clear();
        paymentMethodChoiceBox.getItems().clear();
        arbitratorChoiceBox.getItems().clear();
        minTradeAmtTextField.clear();
        maxTradeAmtTextField.clear();
        btcPriceTextField.clear();
    }

    private SellOffer createOffer(String sellerProfilePubKey, String sellerEscrowPubKey) {

        String arbitratorProfilePubKey = arbitratorChoiceBox.selectionModelProperty().getValue().getSelectedItem().getPubKey();
        CurrencyCode currencyCode = currencyChoiceBox.selectionModelProperty().getValue().getSelectedItem();
        PaymentMethod paymentMethod = paymentMethodChoiceBox.selectionModelProperty().getValue().getSelectedItem();
        BigDecimal maxAmount = new BigDecimal(maxTradeAmtTextField.getText());
        BigDecimal minAmount = new BigDecimal(minTradeAmtTextField.getText());
        BigDecimal price = new BigDecimal(btcPriceTextField.getText());

        return SellOffer.builder()
                .sellerProfilePubKey(sellerProfilePubKey)
                .sellerEscrowPubKey(sellerEscrowPubKey)
                .arbitratorProfilePubKey(arbitratorProfilePubKey)
                .currencyCode(currencyCode)
                .paymentMethod(paymentMethod)
                .maxAmount(minAmount)
                .minAmount(maxAmount)
                .price(price)
                .build();
    }

    // Event classes

    interface PresenterEvent extends Event {
    }

    private class ViewShowing implements PresenterEvent {
    }

    private class ViewNotShowing implements PresenterEvent {
    }

    private class AddButtonPressed implements PresenterEvent {
    }

    private class CurrencyCodeSelected implements PresenterEvent {
        private final CurrencyCode currencyCode;

        public CurrencyCodeSelected(CurrencyCode currencyCode) {
            this.currencyCode = currencyCode;
        }

        public CurrencyCode getCurrencyCode() {
            return currencyCode;
        }
    }

    private class PaymentMethodSelected implements PresenterEvent {
        private final PaymentMethod paymentMethod;

        public PaymentMethodSelected(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }
    }
}
