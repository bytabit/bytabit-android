package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.*;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.util.Optional;

public class ProfileManager extends AbstractManager {

    private String PROFILE_PUBKEY = "profile.pubkey";
    private String PROFILE_ISARBITRATOR = "profile.isArbitrator";
    private String PROFILE_USERNAME = "profile.name";
    private String PROFILE_PHONENUM = "profile.phoneNum";

    private String PROFILE_PAYMENT_DETAILS = "profile.paymentDetails";

    private final ProfileService profilesService;

    private final EventLogger eventLogger = EventLogger.of(ProfileManager.class);

    private final PublishSubject<ProfileAction> actions;

    private final Observable<ProfileResult> results;

    public ProfileManager() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(new JacksonJrConverter<>(Profile.class))
                .build();

        profilesService = retrofit.create(ProfileService.class);

        actions = PublishSubject.create();

        Observable<ProfileAction> actionObservable = actions
                .compose(eventLogger.logEvents())
                .share();

        // profile actions to results

        Observable<ProfileResult> profileLoadedResults = actionObservable
                .ofType(LoadProfile.class)
                .filter(a -> retrieve(PROFILE_PUBKEY).isPresent())
                .flatMap(a -> loadMyProfile().map(ProfileLoaded::new));

        Observable<ProfileResult> profileNotCreatedResults = actionObservable
                .ofType(LoadProfile.class)
                .filter(a -> !retrieve(PROFILE_PUBKEY).isPresent())
                .map(a -> new ProfileNotCreated());

        // wallet results to profile results
//        Observable<ProfileCreated> profileCreatedResults = walletManager.getResults()
//                .ofType(WalletManager.TradeWalletProfilePubKey.class)
//                .map(r -> createMyProfile(r.getPubKey()))
//                .flatMap(this::storeMyProfile)
//                .flatMap(profile -> profilesService.putProfile(profile.getPubKey(), profile).toObservable())
//                .map(ProfileCreated::new);

        Observable<ProfileResult> profileCreatedResults = actionObservable
                .ofType(CreateProfile.class)
                .map(a -> createMyProfile(a.getPubKey()))
                .map(this::storeMyProfile)
                .flatMap(profile -> profilesService.putProfile(profile.getPubKey(), profile).toObservable())
                .map(ProfileCreated::new);

        // profile actions to wallet actions
//        Observable<WalletManager.GetTradeWalletProfilePubKey> getTradeWalletProfilePubKey =
//                actionObservable.ofType(LoadProfile.class)
//                        .filter(a -> !retrieve(PROFILE_PUBKEY).isPresent())
//                        .map(a -> walletManager.new GetTradeWalletProfilePubKey());
//
//        getTradeWalletProfilePubKey.subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .compose(eventLogger.logEvents())
//                .subscribe(walletManager.getActions());

        Observable<ProfileResult> updateProfileResults = actionObservable
                .ofType(UpdateProfile.class)
                .flatMap(a -> loadMyProfile()
                        .map(oldProfile -> updateMyProfile(oldProfile, a.getProfile()))
                        .map(this::storeMyProfile)
                        .flatMap(profile -> profilesService.putProfile(profile.getPubKey(), profile).toObservable())
                        .map(ProfileUpdated::new)
                );

//        ObservableTransformer<UpdateProfile, ProfileResult> myProfileActionTransformer = actions ->
//                actions.compose(eventLogger.logEvents()).flatMap(action -> {
//                    switch (action.getType()) {
//                        case LOAD:
//                            if (!retrieve(PROFILE_PUBKEY).isPresent()) {
//                                return createMyProfile()
//                                        .flatMap(this::storeMyProfile)
//                                        .flatMap(profile -> profilesService.putProfile(profile.getPubKey(), profile).toObservable())
//                                        .map(ProfileResult::created);
//                            } else {
//                                return loadMyProfile()
//                                        .map(ProfileResult::loaded);
//                            }
//                        case UPDATE:
//                            return loadMyProfile()
//                                    .map(oldProfile -> updateMyProfile(oldProfile, action.getProfile()))
//                                    .flatMap(this::storeMyProfile)
//                                    .flatMap(profile -> profilesService.putProfile(profile.getPubKey(), profile).toObservable())
//                                    .map(ProfileResult::updated);
//                        default:
//                            throw new RuntimeException(String.format("Unexpected ProfileAction.Type: %s", action.getType()));
//                    }
//                }).onErrorReturn(ProfileResult::error)
//                        .compose(eventLogger.logResults())
//                        .startWith(ProfileResult.pending());

//        Observable<ProfileResult> loadedProfileResults = actions
//                .filter(LoadProfile.class::isInstance)
//                .map(LoadProfile.class::cast)
//                .flatMap(a -> {
//                    if (!retrieve(PROFILE_PUBKEY).isPresent()) {
//                        return createMyProfile()
//                                .flatMap(this::storeMyProfile)
//                                .flatMap(profile -> profilesService.putProfile(profile.getPubKey(), profile).toObservable())
//                                .map(ProfileResult::created);
//                    } else {
//                        return loadMyProfile()
//                                .map(ProfileResult::loaded);
//                    }
//                });

//        results = actions.compose(myProfileActionTransformer);

        // payment details

//        paymentDetailsActions = PublishSubject.create();

        // payment details actions to results

        Observable<ProfileResult> loadPaymentDetailsResults = actionObservable
                .ofType(LoadPaymentDetails.class)
                .flatMap(a -> loadPaymentDetails())
                .map(PaymentDetailsLoaded::new);

        Observable<ProfileResult> updatePaymentDetailsResults = actionObservable
                .filter(a -> a instanceof UpdatePaymentDetails)
                .ofType(UpdatePaymentDetails.class)
                .map(a -> a.getPaymentDetails())
                .flatMap(this::storePaymentDetails)
                .map(PaymentDetailsUpdated::new);

//        ObservableTransformer<PaymentDetailsAction, PaymentDetailsResult> paymentDetailsActionTransformer = actions ->
//                actions.distinctUntilChanged().compose(eventLogger.logEvents()).flatMap(action -> {
//                    switch (action.getType()) {
//                        case LOAD:
//                            return loadPaymentDetails()
//                                    .map(PaymentDetailsResult::loaded);
//                        case UPDATE:
//                            return storePaymentDetails(action.getPaymentDetails())
//                                    .map(PaymentDetailsResult::updated);
//                        default:
//                            throw new RuntimeException(String.format("Unexpected PaymentDetailsAction.Type: %s", action.getType()));
//                    }
//                }).onErrorReturn(PaymentDetailsResult::error)
//                        .compose(eventLogger.logResults())
//                        .startWith(PaymentDetailsResult.pending());
//
//        paymentDetailsResults = paymentDetailsActions.compose(paymentDetailsActionTransformer)
//                .share();

        results = profileLoadedResults
                .mergeWith(profileNotCreatedResults)
                .mergeWith(profileCreatedResults)
                .mergeWith(updateProfileResults)
                .mergeWith(loadPaymentDetailsResults)
                .mergeWith(updatePaymentDetailsResults)
                .onErrorReturn(e -> new ProfileError(e))
                .startWith(new ProfilePending())
                .compose(eventLogger.logResults())
                .share();
    }

    // my profile

    private Profile updateMyProfile(Profile oldProfile, Profile newProfile) {
        return Profile.builder()
                .pubKey(oldProfile.getPubKey())
                .isArbitrator(newProfile.getIsArbitrator())
                .userName(newProfile.getUserName())
                .phoneNum(newProfile.getPhoneNum())
                .build();
    }

    private Profile storeMyProfile(Profile profile) {

        store(PROFILE_PUBKEY, profile.getPubKey());
        store(PROFILE_ISARBITRATOR, profile.getIsArbitrator().toString());
        store(PROFILE_USERNAME, profile.getUserName());
        store(PROFILE_PHONENUM, profile.getPhoneNum());
        return profile;
    }

    private Profile createMyProfile(String authPubKey) {
        return Profile.builder()
                .pubKey(authPubKey)
                .isArbitrator(Boolean.FALSE)
                .build();
    }

    private Observable<Profile> loadMyProfile() {

        return Observable.fromCallable(() -> Profile.builder()
                .pubKey(retrieve(PROFILE_PUBKEY).get())
                .isArbitrator(Boolean.valueOf(retrieve(PROFILE_ISARBITRATOR).orElse(Boolean.FALSE.toString())))
                .userName(retrieve(PROFILE_USERNAME).orElse(""))
                .phoneNum(retrieve(PROFILE_PHONENUM).orElse(""))
                .build()).subscribeOn(Schedulers.io());
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

    public PublishSubject<ProfileAction> getActions() {
        return actions;
    }

    public Observable<ProfileResult> getResults() {
        return results;
    }

    // Profile Action classes

    public interface ProfileAction extends Event {
    }

    public class LoadProfile implements ProfileAction {
    }

    public class CreateProfile implements ProfileAction {
        private final String pubKey;

        public CreateProfile(String pubKey) {
            this.pubKey = pubKey;
        }

        public String getPubKey() {
            return pubKey;
        }
    }

    public class UpdateProfile implements ProfileAction {
        private final Profile profile;

        public UpdateProfile(Profile profile) {
            this.profile = profile;
        }

        public Profile getProfile() {
            return profile;
        }
    }

    public class LoadPaymentDetails implements ProfileAction {
    }

    public class UpdatePaymentDetails implements ProfileAction {
        private final PaymentDetails paymentDetails;

        public UpdatePaymentDetails(PaymentDetails paymentDetails) {
            this.paymentDetails = paymentDetails;
        }

        public PaymentDetails getPaymentDetails() {
            return paymentDetails;
        }
    }

    // Profile Result classes

    public interface ProfileResult extends Result {
    }

    public class ProfilePending implements ProfileResult {
    }

    public class ProfileNotCreated implements ProfileResult {
    }

    public class ProfileCreated implements ProfileResult {
        private final Profile profile;

        public ProfileCreated(Profile profile) {
            this.profile = profile;
        }

        public Profile getProfile() {
            return profile;
        }
    }

    public class ProfileLoaded implements ProfileResult {
        private final Profile profile;

        public ProfileLoaded(Profile profile) {
            this.profile = profile;
        }

        public Profile getProfile() {
            return profile;
        }
    }

    public class ProfileUpdated implements ProfileResult {
        private final Profile profile;

        public ProfileUpdated(Profile profile) {
            this.profile = profile;
        }

        public Profile getProfile() {
            return profile;
        }
    }

    public class ProfileError implements ProfileResult, ErrorResult {
        private final Throwable error;

        public ProfileError(Throwable error) {
            this.error = error;
        }

        @Override
        public Throwable getError() {
            return error;
        }
    }

    public class PaymentDetailsPending implements ProfileResult {
    }

    public class PaymentDetailsLoaded implements ProfileResult {
        private final PaymentDetails paymentDetails;

        public PaymentDetailsLoaded(PaymentDetails paymentDetails) {
            this.paymentDetails = paymentDetails;
        }

        public PaymentDetails getPaymentDetails() {
            return paymentDetails;
        }
    }

    public class PaymentDetailsUpdated implements ProfileResult {
        private final PaymentDetails paymentDetails;

        public PaymentDetailsUpdated(PaymentDetails paymentDetails) {
            this.paymentDetails = paymentDetails;
        }

        public PaymentDetails getPaymentDetails() {
            return paymentDetails;
        }
    }

    public class PaymentDetailsError implements ProfileResult {
        private final Throwable error;

        public PaymentDetailsError(Throwable error) {
            this.error = error;
        }

        public Throwable getError() {
            return error;
        }
    }
}