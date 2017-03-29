package com.bytabit.mobile.profile;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ProfileManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(ProfileManager.class);

    private String PROFILE_PUBKEY = "profile.pubkey";
    private String PROFILE_ISARBITRATOR = "profile.isArbitrator";
    private String PROFILE_NAME = "profile.name";
    private String PROFILE_PHONENUM = "profile.phoneNum";
    private String PROFILE_PAYMENT_DETAILS = "profile.paymentDetails";

    private final ProfileService profileService;
    private final Profile profile;

    private final PaymentDetails newPaymentDetails;
    private final ObservableList<PaymentDetails> paymentDetails;

    private final ObservableList<Profile> otherProfiles;

    public ProfileManager() {
        super();
        profileService = retrofit.create(ProfileService.class);

        // profile

        profile = new Profile(retrieve(PROFILE_PUBKEY).orElse(null),
                retrieve(PROFILE_ISARBITRATOR).map(Boolean::parseBoolean).orElse(Boolean.FALSE),
                retrieve(PROFILE_NAME).orElse(""),
                retrieve(PROFILE_PHONENUM).orElse("")
        );

        profile.isArbitratorProperty().addListener((obj, oldVal, newVal) -> {
            store(PROFILE_ISARBITRATOR, newVal.toString());
            updateProfile();
        });

        profile.nameProperty().addListener((obj, oldVal, newVal) -> {
            store(PROFILE_NAME, newVal);
            updateProfile();
        });

        profile.phoneNumProperty().addListener((obj, oldVal, newVal) -> {
            store(PROFILE_PHONENUM, newVal);
            updateProfile();
        });

        // payment details

        newPaymentDetails = new PaymentDetails(null, null, null);

        paymentDetails = FXCollections.observableArrayList();
        paymentDetails.addAll(retrievePaymentDetails());

        // other profiles

        otherProfiles = FXCollections.observableArrayList();
    }

    // profile methods

    public void createProfile(String pubKey) {
        store(PROFILE_PUBKEY, pubKey);
        profile.setPubKey(pubKey);
        try {
            profileService.create(profile).execute();
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
        }
    }

    public Profile profile() {
        return profile;
    }

    private void updateProfile() {
        try {
            profileService.update(profile.getPubKey(), profile).execute();
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
        }
    }

    // payment details methods

    public ObservableList<PaymentDetails> paymentDetails() {
        return paymentDetails;
    }

    public PaymentDetails newPaymentDetails() {
        return newPaymentDetails;
    }

    public List<CurrencyCode> currencyCodes() {
        Set<CurrencyCode> currencyCodes = new HashSet<>();
        for (PaymentDetails d : paymentDetails()) {
            currencyCodes.add(d.getCurrencyCode());
        }
        List<CurrencyCode> codes = new ArrayList<>();
        codes.addAll(currencyCodes);
        return codes;
    }

    public List<PaymentMethod> paymentMethods(CurrencyCode currencyCode) {
        List<PaymentMethod> paymentMethods = new ArrayList<>();
        for (PaymentDetails d : paymentDetails()) {
            if (d.getCurrencyCode().equals(currencyCode)) {
                paymentMethods.add(d.getPaymentMethod());
            }
        }
        return paymentMethods;
    }

    public void addPaymentDetails() {
        CurrencyCode cc = newPaymentDetails.getCurrencyCode();
        PaymentMethod pm = newPaymentDetails.getPaymentMethod();
        String pd = newPaymentDetails.getPaymentDetails();

        if (cc != null && pm != null && pd.length() > 0) {
            store(paymentDetailsKey(cc, pm), pd);
            paymentDetails.removeIf(p -> p.getCurrencyCode().equals(cc) && p.getPaymentMethod().equals(pm));
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

    private Optional<String> retrievePaymentDetails(CurrencyCode currencyCode,
                                                    PaymentMethod paymentMethod) {

        return retrieve(paymentDetailsKey(currencyCode, paymentMethod));
    }

    private String paymentDetailsKey(CurrencyCode currencyCode,
                                     PaymentMethod paymentMethod) {

        return String.format("%s.%s.%s", PROFILE_PAYMENT_DETAILS, currencyCode.name(),
                paymentMethod.displayName());
    }
}