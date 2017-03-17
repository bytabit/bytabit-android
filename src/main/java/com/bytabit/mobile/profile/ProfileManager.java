package com.bytabit.mobile.profile;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProfileManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(ProfileManager.class);

    private String PROFILE_PUBKEY = "profile.pubkey";
    private String PROFILE_ISARBITRATOR = "profile.isArbitrator";
    private String PROFILE_NAME = "profile.name";
    private String PROFILE_PHONENUM = "profile.phoneNum";
    private String PROFILE_PAYMENTDTLS = "profile.paymentDtls";

    private final ProfileService profileService;

    public ProfileManager() {
        super();
        profileService = retrofit.create(ProfileService.class);
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