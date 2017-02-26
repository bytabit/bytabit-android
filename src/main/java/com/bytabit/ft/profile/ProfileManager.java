package com.bytabit.ft.profile;

import com.bytabit.ft.profile.model.CurrencyCode;
import com.bytabit.ft.profile.model.PaymentDetails;
import com.bytabit.ft.profile.model.PaymentMethod;
import com.bytabit.ft.profile.model.Profile;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProfileManager {

    private static Logger LOG = LoggerFactory.getLogger(ProfileManager.class);

    private String PROFILE_PUBKEY = "profile.pubkey";
    private String PROFILE_ISARBITRATOR = "profile.isArbitrator";
    private String PROFILE_NAME = "profile.name";
    private String PROFILE_PHONENUM = "profile.phoneNum";
    private String PROFILE_PAYMENTDTLS = "profile.paymentDtls";

    private final Retrofit retrofit;
    private final ProfileService profileService;

    public ProfileManager() {
        retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:8080")
                .addConverterFactory(new JacksonJrConverter<Profile>(Profile.class))
                .build();

        profileService = retrofit.create(ProfileService.class);
    }

    private Optional<String> retrieve(String key) {
        return Services.get(SettingsService.class).map(s -> s.retrieve(key));
    }

    private void store(String key, String value) {
        Services.get(SettingsService.class).ifPresent(s -> s.store(key, value));
    }

    private void remove(String key) {
        Services.get(SettingsService.class).ifPresent(s -> s.remove(key));
    }

    public Optional<String> getPubKey() {
        return retrieve(PROFILE_PUBKEY);
    }

    public void setPubKey(String pubKey) {
        store(PROFILE_PUBKEY, pubKey);
        createProfile(pubKey);
    }

    public void setIsArbitrator(Boolean isArbitrator) {
        store(PROFILE_ISARBITRATOR, isArbitrator.toString());
        updateProfile();
    }

    public Optional<Boolean> isArbitrator() {
        return retrieve(PROFILE_ISARBITRATOR).map(Boolean::parseBoolean);
    }

    public Optional<String> getName() {
        return retrieve(PROFILE_NAME);
    }

    public void setName(String name) {
        store(PROFILE_NAME, name);
        updateProfile();
    }

    public Optional<String> getPhoneNum() {
        return retrieve(PROFILE_PHONENUM);
    }

    public void setPhoneNum(String phoneNum) {
        store(PROFILE_PHONENUM, phoneNum);
        updateProfile();
    }

    public Optional<String> getPaymentDetails(CurrencyCode currencyCode,
                                              PaymentMethod paymentMethod) {

        return retrieve(paymentDetailsKey(currencyCode, paymentMethod));
    }

    public void setPaymentDetails(CurrencyCode currencyCode,
                                  PaymentMethod paymentMethod,
                                  String paymentDetails) {

        String key = paymentDetailsKey(currencyCode, paymentMethod);
        //retrieve(key).ifPresent(pd -> remove(key));
        store(paymentDetailsKey(currencyCode, paymentMethod), paymentDetails);
    }

    private String paymentDetailsKey(CurrencyCode currencyCode,
                                     PaymentMethod paymentMethod) {

        return String.format("%s.%s.%s", PROFILE_PAYMENTDTLS, currencyCode.name(),
                paymentMethod.displayName());
    }

    public List<PaymentDetails> getPaymentDetails() {
        List<PaymentDetails> paymentDetails = new ArrayList<PaymentDetails>();
        for (CurrencyCode c : CurrencyCode.values()) {
            for (PaymentMethod p : c.paymentMethods()) {
                getPaymentDetails(c, p).ifPresent(pd -> {
                    paymentDetails.add(new PaymentDetails(c, p, pd));
                });
            }
        }
        return paymentDetails;
    }

    public void createProfile(String pubKey) {
        Profile profile = new Profile(pubKey, null, null, null);
        try {
            profileService.createProfile(profile).execute();
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
        }
    }

    public void updateProfile() {
        String pubKey = getPubKey().get();
        Boolean isArbitrator = isArbitrator().orElse(null);
        String name = getName().orElse(null);
        String phoneNum = getPhoneNum().orElse(null);
        Profile profile = new Profile(null, isArbitrator, name, phoneNum);

        try {
            profileService.updateProfile(pubKey, profile).execute();
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
        }
    }
}