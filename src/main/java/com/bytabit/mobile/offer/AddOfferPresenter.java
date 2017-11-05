package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.StringBigDecimalConverter;
import com.bytabit.mobile.profile.PaymentMethodStringConverter;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.profile.ProfileStringConverter;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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

    private static Logger LOG = LoggerFactory.getLogger(AddOfferPresenter.class);

    @Inject
    OfferManager offerManager;

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

    final private StringProperty myProfilePubKeyProperty = new SimpleStringProperty();

    public void initialize() {

        LOG.debug("initialize add offer presenter");

        StringConverter<BigDecimal> bigDecConverter = new StringBigDecimalConverter();

        arbitratorChoiceBox.setConverter(new ProfileStringConverter());

        addOfferView.showingProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
                appBar.setTitleText("Create Sell Offer");

                profileManager.getCurrencyCodes().observeOn(JavaFxScheduler.platform()).subscribe(cl -> {
                    currencyChoiceBox.getItems().setAll(cl);
                    currencyChoiceBox.getSelectionModel().select(0);
                });

                // TODO myProfile = profileManager.retrieveProfile();
                profileManager.getArbitratorProfiles().observeOn(JavaFxScheduler.platform())
                        .subscribe(al -> {
                            arbitratorChoiceBox.getItems().setAll(al);
                            arbitratorChoiceBox.getSelectionModel().select(0);
                        });

                paymentMethodChoiceBox.requestFocus();

                profileManager.retrieveMyProfile().observeOn(JavaFxScheduler.platform())
                        .subscribe(p -> myProfilePubKeyProperty.setValue(p.getPubKey()));
            }
        });

        paymentMethodChoiceBox.setConverter(new PaymentMethodStringConverter());

        currencyChoiceBox.getSelectionModel().selectedItemProperty().addListener((obj, ov, currencyCode) -> {
            if (currencyCode != null) {
                profileManager.getPaymentMethods(currencyCode).observeOn(JavaFxScheduler.platform())
                        .subscribe(pl -> {
                            paymentMethodChoiceBox.getItems().setAll(pl);
                            paymentMethodChoiceBox.getSelectionModel().select(0);
                            minTradeAmtCurrencyLabel.textProperty().setValue(currencyCode.name());
                            maxTradeAmtCurrencyLabel.textProperty().setValue(currencyCode.name());
                            btcPriceCurrencyLabel.textProperty().setValue(currencyCode.name());
                        });
            }
        });

        addOfferButton.onActionProperty().setValue(e -> {
            offerManager.createOffer(currencyChoiceBox.getValue(), paymentMethodChoiceBox.getValue(), arbitratorChoiceBox.getValue(),
                    bigDecConverter.fromString(minTradeAmtTextField.textProperty().get()),
                    bigDecConverter.fromString(maxTradeAmtTextField.textProperty().get()),
                    bigDecConverter.fromString(btcPriceTextField.textProperty().get()))
                    .observeOn(JavaFxScheduler.platform()).map(so -> offerManager.offers.add(so)).subscribe();
            MobileApplication.getInstance().switchToPreviousView();
        });
    }
}
