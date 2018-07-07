package com.bytabit.mobile.offer.ui;

import com.bytabit.mobile.common.DecimalTextFieldFormatter;
import com.bytabit.mobile.common.StringBigDecimalConverter;
import com.bytabit.mobile.offer.manager.OfferManager;
import com.bytabit.mobile.profile.manager.PaymentDetailsManager;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;

public class AddOfferPresenter {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    OfferManager offerManager;

    @Inject
    PaymentDetailsManager paymentDetailsManager;

    @Inject
    ProfileManager profileManager;

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

    public void initialize() {

        // setup view components

        StringConverter<BigDecimal> bigDecConverter = new StringBigDecimalConverter();

        paymentMethodChoiceBox.setConverter(new PaymentMethodStringConverter());

        arbitratorChoiceBox.setConverter(new ProfileStringConverter());

        minTradeAmtTextField.setTextFormatter(new DecimalTextFieldFormatter());
        maxTradeAmtTextField.setTextFormatter(new DecimalTextFieldFormatter());
        btcPriceTextField.setTextFormatter(new DecimalTextFieldFormatter());

        // setup event observables

        JavaFxObservable.changesOf(addOfferView.showingProperty())
                .filter(Change::getNewVal)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(event -> {
                    setAppBar();
                    clearForm();
                });

        Observable.create(source ->
                addOfferButton.setOnAction(source::onNext))
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(sellOffer -> {
                    CurrencyCode currencyCode = currencyChoiceBox.selectionModelProperty().getValue().getSelectedItem();
                    PaymentMethod paymentMethod = paymentMethodChoiceBox.selectionModelProperty().getValue().getSelectedItem();
                    String arbitratorProfilePubKey = arbitratorChoiceBox.selectionModelProperty().getValue().getSelectedItem().getPubKey();
                    BigDecimal maxAmount = new BigDecimal(maxTradeAmtTextField.getText());
                    BigDecimal minAmount = new BigDecimal(minTradeAmtTextField.getText());
                    BigDecimal price = new BigDecimal(btcPriceTextField.getText());
                    offerManager.createOffer(currencyCode, paymentMethod, arbitratorProfilePubKey, minAmount, maxAmount, price);
                    MobileApplication.getInstance().switchToPreviousView();

                });

        JavaFxObservable.changesOf(currencyChoiceBox.getSelectionModel().selectedItemProperty())
                .map(Change::getNewVal)
                .flatMap(cc -> paymentDetailsManager.getLoadedPaymentDetails()
                        .filter(p -> p.getCurrencyCode().equals(cc))
                        .map(PaymentDetails::getPaymentMethod).sorted()
                        .toList().toObservable())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(pm -> {
                    paymentMethodChoiceBox.itemsProperty().getValue().setAll(pm);
                    paymentMethodChoiceBox.getSelectionModel().selectFirst();
                });

        JavaFxObservable.changesOf(currencyChoiceBox.getSelectionModel().selectedItemProperty())
                .map(Change::getNewVal)
                .map(CurrencyCode::toString)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(cc -> {
                    minTradeAmtCurrencyLabel.setText(cc);
                    maxTradeAmtCurrencyLabel.setText(cc);
                    btcPriceCurrencyLabel.setText(cc);
                });

        Observable.concat(paymentDetailsManager.getLoadedPaymentDetails(),
                paymentDetailsManager.getUpdatedPaymentDetails())
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(paymentDetails -> {
                    CurrencyCode cc = paymentDetails.getCurrencyCode();
                    PaymentMethod pm = paymentDetails.getPaymentMethod();
                    if (!currencyChoiceBox.getItems().contains(cc)) {
                        currencyChoiceBox.getItems().add(cc);
                    }
                    if (currencyChoiceBox.getSelectionModel().isEmpty()) {
                        currencyChoiceBox.getSelectionModel().selectFirst();
                    }
                    if (currencyChoiceBox.getSelectionModel().getSelectedItem().equals(cc) &&
                            !paymentMethodChoiceBox.getItems().contains(pm)) {
                        paymentMethodChoiceBox.getItems().add(pm);
                        if (paymentMethodChoiceBox.getSelectionModel().isEmpty()) {
                            paymentMethodChoiceBox.getSelectionModel().selectFirst();
                        }
                    }
                });

        paymentDetailsManager.getRemovedPaymentDetails()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(pd -> {
                    CurrencyCode cc = pd.getCurrencyCode();
                    PaymentMethod pm = pd.getPaymentMethod();
                    if (currencyChoiceBox.getSelectionModel().getSelectedItem().equals(cc)) {
                        paymentMethodChoiceBox.getItems().remove(pm);
                        if (paymentMethodChoiceBox.getItems().isEmpty()) {
                            currencyChoiceBox.getItems().remove(cc);
                            currencyChoiceBox.getSelectionModel().selectFirst();
                        } else if (paymentMethodChoiceBox.getSelectionModel().isEmpty()) {
                            paymentMethodChoiceBox.getSelectionModel().selectFirst();
                        }
                    }
                });

        profileManager.loadArbitratorProfiles()
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .flatMap(pl -> Observable.fromIterable(pl).filter(Profile::isArbitrator).toList().toObservable())
                .subscribe(al -> {
                    arbitratorChoiceBox.getItems().setAll(al);
                    Profile selected = arbitratorChoiceBox.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        arbitratorChoiceBox.getSelectionModel().select(selected);
                    } else {
                        arbitratorChoiceBox.getSelectionModel().selectFirst();
                    }
                });
    }

    private void setAppBar() {
        AppBar appBar = MobileApplication.getInstance().getAppBar();
        appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
        appBar.setTitleText("Add Sell Offer");
    }

    private void clearForm() {
        if (currencyChoiceBox.getSelectionModel().isEmpty()) {
            currencyChoiceBox.getSelectionModel().selectFirst();
        }
        minTradeAmtTextField.clear();
        maxTradeAmtTextField.clear();
        btcPriceTextField.clear();
    }
}
