package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.wallet.WalletManager;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import javax.inject.Inject;
import java.util.Optional;

public class ProfileManager extends AbstractManager {

    @Inject
    private WalletManager walletManager;

    private String PROFILE_PUBKEY = "profile.pubkey";
    private String PROFILE_ISARBITRATOR = "profile.isArbitrator";
    private String PROFILE_USERNAME = "profile.name";
    private String PROFILE_PHONENUM = "profile.phoneNum";

    private String PROFILE_PAYMENT_DETAILS = "profile.paymentDetails";

    private final ProfileService profilesService;

    private final EventLogger eventLogger = EventLogger.of(ProfileManager.class);

    // profile

    private final PublishSubject<ProfileAction> myProfileActions;

    private final Observable<ProfileResult> myProfileResults;

    // payment details

    private final PublishSubject<PaymentDetailsAction> paymentDetailsActions;

    private final Observable<PaymentDetailsResult> paymentDetailsResults;

    public ProfileManager() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(new JacksonJrConverter<>(Profile.class))
                .build();

        profilesService = retrofit.create(ProfileService.class);

        myProfileActions = PublishSubject.create();

        ObservableTransformer<ProfileAction, ProfileResult> myProfileActionTransformer = actions ->
                actions.compose(eventLogger.logActions()).flatMap(action -> {
                    switch (action.getType()) {
                        case LOAD:
                            if (!retrieve(PROFILE_PUBKEY).isPresent()) {
                                return createMyProfile()
                                        .flatMap(this::storeMyProfile)
                                        .flatMap(profile -> profilesService.putProfile(profile.getPubKey(), profile).toObservable())
                                        .map(ProfileResult::created);
                            } else {
                                return loadMyProfile()
                                        .map(ProfileResult::loaded);
                            }
                        case UPDATE:
                            return loadMyProfile()
                                    .map(oldProfile -> updateMyProfile(oldProfile, action.getData()))
                                    .flatMap(this::storeMyProfile)
                                    .flatMap(profile -> profilesService.putProfile(profile.getPubKey(), profile).toObservable())
                                    .map(ProfileResult::updated);
                        default:
                            throw new RuntimeException(String.format("Unexpected ProfileAction.Type: %s", action.getType()));
                    }
                }).onErrorReturn(ProfileResult::error)
                        .compose(eventLogger.logResults())
                        .startWith(ProfileResult.pending());

        myProfileResults = myProfileActions.compose(myProfileActionTransformer);

        // payment details

        paymentDetailsActions = PublishSubject.create();

        ObservableTransformer<PaymentDetailsAction, PaymentDetailsResult> paymentDetailsActionTransformer = actions ->
                actions.compose(eventLogger.logActions()).flatMap(action -> {
                    switch (action.getType()) {
                        case LOAD:
                            return loadPaymentDetails()
                                    .map(PaymentDetailsResult::loaded);
                        case ADD:
                            return storePaymentDetails(action.getData())
                                    .map(PaymentDetailsResult::added);
                        case UPDATE:
                            return storePaymentDetails(action.getData())
                                    .map(PaymentDetailsResult::updated);
                        default:
                            throw new RuntimeException(String.format("Unexpected PaymentDetailsAction.Type: %s", action.getType()));
                    }
                }).onErrorReturn(PaymentDetailsResult::error)
                        .compose(eventLogger.logResults())
                        .startWith(PaymentDetailsResult.pending());

        paymentDetailsResults = paymentDetailsActions.compose(paymentDetailsActionTransformer);
    }

    // my profile

    public PublishSubject<ProfileAction> getMyProfileActions() {
        return myProfileActions;
    }

    public Observable<ProfileResult> getMyProfileResults() {
        return myProfileResults;
    }

    private Profile updateMyProfile(Profile oldProfile, Profile newProfile) {
        return Profile.builder()
                .pubKey(oldProfile.getPubKey())
                .isArbitrator(newProfile.getIsArbitrator())
                .userName(newProfile.getUserName())
                .phoneNum(newProfile.getPhoneNum())
                .build();
    }

    private Observable<Profile> storeMyProfile(Profile profile) {
        return profilesService.putProfile(profile.getPubKey(), profile).map(p -> {
            store(PROFILE_PUBKEY, p.getPubKey());
            store(PROFILE_ISARBITRATOR, p.getIsArbitrator().toString());
            store(PROFILE_USERNAME, p.getUserName());
            store(PROFILE_PHONENUM, p.getPhoneNum());
            return profile;
        }).toObservable().subscribeOn(Schedulers.io());
    }

    private Observable<Profile> createMyProfile() {
        return walletManager.getFreshBase58AuthPubKey().toObservable()
                .map(pk -> Profile.builder()
                        .pubKey(pk)
                        .isArbitrator(Boolean.FALSE)
                        .userName(null)
                        .phoneNum(null)
                        .build());
    }

    private Observable<Profile> loadMyProfile() {

        return Observable.fromCallable(() -> Profile.builder()
                .pubKey(retrieve(PROFILE_PUBKEY).get())
                .isArbitrator(Boolean.valueOf(retrieve(PROFILE_ISARBITRATOR).orElse(Boolean.FALSE.toString())))
                .userName(retrieve(PROFILE_USERNAME).orElse(""))
                .phoneNum(retrieve(PROFILE_PHONENUM).orElse(""))
                .build()).subscribeOn(Schedulers.io());
    }

//    private Single<List<Profile>> getArbitratorProfiles() {
//        return profilesService.getProfiles()
//                .flattenAsObservable(pl -> pl)
//                .filter(Profile::getIsArbitrator)
//                .toList();
//    }

    // payment details

    public PublishSubject<PaymentDetailsAction> getPaymentDetailsActions() {
        return paymentDetailsActions;
    }

    public Observable<PaymentDetailsResult> getPaymentDetailsResults() {
        return paymentDetailsResults;
    }

    private Observable<PaymentDetails> storePaymentDetails(PaymentDetails paymentDetails) {
        return Observable.fromCallable(() -> {
                    store(paymentDetailsKey(paymentDetails.getCurrencyCode(),
                            paymentDetails.getPaymentMethod()), paymentDetails.getPaymentDetails());
                    return paymentDetails;
                }
        ).subscribeOn(Schedulers.io());
    }

    private Observable<PaymentDetails> loadPaymentDetails() {

        return Observable.fromArray(CurrencyCode.values())
                .flatMap(c -> Observable.fromIterable(c.paymentMethods())
                        .map(p -> retrieve(paymentDetailsKey(c, p)).map(pd ->
                                PaymentDetails.builder()
                                        .currencyCode(c)
                                        .paymentMethod(p)
                                        .paymentDetails(pd).build()))
                        .filter(Optional::isPresent)
                        .map(Optional::get));
    }

    private String paymentDetailsKey(CurrencyCode currencyCode,
                                     PaymentMethod paymentMethod) {

        return String.format("%s.%s.%s", PROFILE_PAYMENT_DETAILS, currencyCode.name(),
                paymentMethod.displayName());
    }
}