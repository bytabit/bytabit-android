package com.bytabit.mobile.profile;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.wallet.WalletManager;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import rx.schedulers.JavaFxScheduler;
import rx.schedulers.Schedulers;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Getter
public class ProfileManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(ProfileManager.class);

    @Inject
    private WalletManager tradeWalletManager;

    private String PROFILE_PUBKEY = "profile.pubkey";
    private String PROFILE_ISARBITRATOR = "profile.isArbitrator";
    private String PROFILE_USERNAME = "profile.name";
    private String PROFILE_PHONENUM = "profile.phoneNum";
    private String PROFILE_PAYMENT_DETAILS = "profile.paymentDetails";

    private final ProfileService profileService;

    // profile properties
    private final StringProperty pubKeyProperty;
    private final BooleanProperty isArbitratorProperty;
    private final StringProperty userNameProperty;
    private final StringProperty phoneNumProperty;

    // new payment details properties
    private final SimpleObjectProperty<CurrencyCode> currencyCodeProperty;
    private final SimpleObjectProperty<PaymentMethod> paymentMethodProperty;
    private final StringProperty paymentDetailsProperty;

    private final ObservableList<PaymentDetails> paymentDetails;
    private final ObservableList<Profile> traderProfiles;
    private final ObservableList<Profile> arbitratorProfiles;


    public ProfileManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(Profile.class))
                .build();

        profileService = retrofit.create(ProfileService.class);

        // profile

        pubKeyProperty = new SimpleStringProperty();
        isArbitratorProperty = new SimpleBooleanProperty();
        userNameProperty = new SimpleStringProperty();
        phoneNumProperty = new SimpleStringProperty();

        pubKeyProperty.setValue(retrieve(PROFILE_PUBKEY).orElse(null));
        isArbitratorProperty.setValue(retrieve(PROFILE_ISARBITRATOR).map(Boolean::parseBoolean).orElse(Boolean.FALSE));
        userNameProperty.setValue(retrieve(PROFILE_USERNAME).orElse(""));
        phoneNumProperty.setValue(retrieve(PROFILE_PHONENUM).orElse(""));

        // payment details

        currencyCodeProperty = new SimpleObjectProperty<>();
        paymentMethodProperty = new SimpleObjectProperty<>();
        paymentDetailsProperty = new SimpleStringProperty();

        paymentDetails = FXCollections.observableArrayList();
        paymentDetails.addAll(retrievePaymentDetails());

        // other profiles

        traderProfiles = FXCollections.observableArrayList();
        arbitratorProfiles = FXCollections.observableArrayList();

        try {
            updateProfiles(profileService.get().execute().body());
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
        }

        rx.Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> profileService.get())
                .retry()
                .observeOn(JavaFxScheduler.getInstance())
                .subscribe(getProfiles -> {
                    try {
                        updateProfiles(getProfiles.execute().body());
                    } catch (IOException ioe) {
                        LOG.error(ioe.getMessage());
                    }
                });
    }

    private void updateProfiles(List<Profile> profiles) {
        Profile found = null;
        for (Profile p : profiles) {
            if (p.getPubKey().equals(pubKeyProperty.getValue())) {
                found = p;
            }
        }
        if (found != null) {
            profiles.remove(found);
        }
        List<Profile> traders = new ArrayList<>();
        List<Profile> arbitrators = new ArrayList<>();
        for (Profile p : profiles) {
            if (p.getIsArbitrator()) {
                arbitrators.add(p);
            } else {
                traders.add(p);
            }
        }
        Platform.runLater(() -> {
            traderProfiles.setAll(traders);
            arbitratorProfiles.setAll(arbitrators);
        });
    }

    // profile methods

    void createProfile() {
        String pubKey = retrieve(PROFILE_PUBKEY).orElse(null);
        if (pubKey == null) {
            pubKey = tradeWalletManager.getFreshBase58AuthPubKey();
            pubKeyProperty.setValue(pubKey);
            store(PROFILE_PUBKEY, pubKey);
            updateProfile();
        }
    }

    void updateProfile() {
        try {
            Profile profile = Profile.builder()
                    .pubKey(pubKeyProperty.getValue())
                    .isArbitrator(isArbitratorProperty.getValue())
                    .userName(userNameProperty.getValue())
                    .phoneNum(phoneNumProperty.getValue())
                    .build();

            store(PROFILE_ISARBITRATOR, isArbitratorProperty.toString());
            store(PROFILE_USERNAME, profile.getUserName());
            store(PROFILE_PHONENUM, profile.getPhoneNum());

            profileService.put(profile.getPubKey(), profile).execute();
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
        }
    }

    // payment details methods

    public List<CurrencyCode> currencyCodes() {
        Set<CurrencyCode> currencyCodes = new HashSet<>();
        for (PaymentDetails d : getPaymentDetails()) {
            currencyCodes.add(d.getCurrencyCode());
        }
        List<CurrencyCode> codes = new ArrayList<>();
        codes.addAll(currencyCodes);
        return codes;
    }

    public List<PaymentMethod> paymentMethods(CurrencyCode currencyCode) {
        List<PaymentMethod> paymentMethods = new ArrayList<>();
        for (PaymentDetails d : getPaymentDetails()) {
            if (d.getCurrencyCode().equals(currencyCode)) {
                paymentMethods.add(d.getPaymentMethod());
            }
        }
        return paymentMethods;
    }

    void addPaymentDetails() {
        CurrencyCode cc = currencyCodeProperty.getValue();
        PaymentMethod pm = paymentMethodProperty.getValue();
        String pd = paymentDetailsProperty.getValue();

        if (cc != null && pm != null && pd.length() > 0) {
            store(paymentDetailsKey(cc, pm), pd);
            PaymentDetails found = null;
            for (PaymentDetails p : paymentDetails) {
                if (p.getCurrencyCode().equals(cc) && p.getPaymentMethod().equals(pm)) {
                    found = p;
                }
            }
            if (found != null) {
                paymentDetails.remove(found);
            }
            paymentDetails.add(new PaymentDetails(cc, pm, pd));
        }
    }

    private List<PaymentDetails> retrievePaymentDetails() {
        List<PaymentDetails> paymentDetails = new ArrayList<>();
        for (CurrencyCode c : CurrencyCode.values()) {
            for (PaymentMethod p : c.paymentMethods()) {
                retrievePaymentDetails(c, p).ifPresent(pd -> {
                    paymentDetails.add(new PaymentDetails(c, p, pd));
                });
            }
        }
        return paymentDetails;
    }

    public Optional<String> retrievePaymentDetails(CurrencyCode currencyCode,
                                                   PaymentMethod paymentMethod) {

        return retrieve(paymentDetailsKey(currencyCode, paymentMethod));
    }

    private String paymentDetailsKey(CurrencyCode currencyCode,
                                     PaymentMethod paymentMethod) {

        return String.format("%s.%s.%s", PROFILE_PAYMENT_DETAILS, currencyCode.name(),
                paymentMethod.displayName());
    }
}