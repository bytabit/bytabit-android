package com.bytabit.mobile.profile;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.wallet.WalletManager;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ProfileManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(ProfileManager.class);

    @Inject
    private WalletManager walletManager;

    private String PROFILE_PUBKEY = "profile.pubkey";
    private String PROFILE_ISARBITRATOR = "profile.isArbitrator";
    private String PROFILE_USERNAME = "profile.name";
    private String PROFILE_PHONENUM = "profile.phoneNum";

    private String PROFILE_PAYMENT_DETAILS = "profile.paymentDetails";

    private final ProfileService profilesService;

    public ProfileManager() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(new JacksonJrConverter<>(Profile.class))
                .build();

        profilesService = retrofit.create(ProfileService.class);
    }

    // my profile

    public Single<Profile> storeMyProfile(Profile profile) {
        return profilesService.putProfile(profile.getPubKey(), profile).map(p -> {
            store(PROFILE_PUBKEY, p.getPubKey());
            store(PROFILE_ISARBITRATOR, p.getIsArbitrator().toString());
            store(PROFILE_USERNAME, p.getUserName());
            store(PROFILE_PHONENUM, p.getPhoneNum());
            return profile;
        }).subscribeOn(Schedulers.io());
    }

    public Single<Profile> retrieveMyProfile() {

        if (!retrieve(PROFILE_PUBKEY).isPresent()) {
            final Profile profile = Profile.builder()
                    .pubKey(walletManager.getFreshBase58AuthPubKey())
                    .isArbitrator(Boolean.FALSE)
                    .userName(null)
                    .phoneNum(null)
                    .build();
            return storeMyProfile(profile);
        } else {
            return Single.fromCallable(() -> Profile.builder()
                    .pubKey(retrieve(PROFILE_PUBKEY).get())
                    .isArbitrator(Boolean.valueOf(retrieve(PROFILE_ISARBITRATOR).orElse(Boolean.FALSE.toString())))
                    .userName(retrieve(PROFILE_USERNAME).orElse(""))
                    .phoneNum(retrieve(PROFILE_PHONENUM).orElse(""))
                    .build()).subscribeOn(Schedulers.io());
        }
    }

    public Single<List<Profile>> getArbitratorProfiles() {
        return profilesService.getProfiles()
                .flattenAsObservable(pl -> pl)
                .filter(Profile::getIsArbitrator)
                .toList();
    }

    // payment details

    public Single<List<CurrencyCode>> getCurrencyCodes() {
        return retrievePaymentDetails().flattenAsObservable(pdl -> pdl)
                .map(PaymentDetails::getCurrencyCode)
                .distinct().toList();
    }

    public Single<List<PaymentMethod>> getPaymentMethods(CurrencyCode currencyCode) {
        return retrievePaymentDetails().flattenAsObservable(pml -> pml)
                .filter(pd -> currencyCode.equals(pd.getCurrencyCode()))
                .map(PaymentDetails::getPaymentMethod)
                .distinct().toList();
    }

    public Single<String> getPaymentDetails(CurrencyCode currencyCode, PaymentMethod paymentMethod) {
        return retrievePaymentDetails().flattenAsObservable(pml -> pml)
                .filter(pd -> currencyCode.equals(pd.getCurrencyCode()) && paymentMethod.equals(pd.getPaymentMethod()))
                .map(PaymentDetails::getPaymentDetails)
                .firstOrError();
    }

    public Single<PaymentDetails> storePaymentDetails(CurrencyCode currencyCode, PaymentMethod paymentMethod, String paymentDetails) {
        return Single.fromCallable(() -> {
            store(paymentDetailsKey(currencyCode, paymentMethod), paymentDetails);
            return new PaymentDetails(currencyCode, paymentMethod, paymentDetails);
                }
        ).subscribeOn(Schedulers.io());
    }

    public Single<List<PaymentDetails>> retrievePaymentDetails() {
        return Single.fromCallable(() -> {
            List<PaymentDetails> paymentDetails = new ArrayList<>();
            for (CurrencyCode c : CurrencyCode.values()) {
                for (PaymentMethod p : c.paymentMethods()) {
                    retrieve(paymentDetailsKey(c, p)).ifPresent(pd ->
                            paymentDetails.add(PaymentDetails.builder().currencyCode(c).paymentMethod(p).paymentDetails(pd).build())
                    );
                }
            }
            return paymentDetails;
        }).subscribeOn(Schedulers.io());
    }

    private String paymentDetailsKey(CurrencyCode currencyCode,
                                     PaymentMethod paymentMethod) {

        return String.format("%s.%s.%s", PROFILE_PAYMENT_DETAILS, currencyCode.name(),
                paymentMethod.displayName());
    }
}