package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.*;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.profile.model.Profile;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import java.util.Optional;

public class ProfileManager extends AbstractManager {

    private String PROFILE_PUBKEY = "profile.pubkey";
    private String PROFILE_ISARBITRATOR = "profile.arbitrator";
    private String PROFILE_USERNAME = "profile.name";
    private String PROFILE_PHONENUM = "profile.phoneNum";

    private String PROFILE_PAYMENT_DETAILS = "profile.paymentDetails";

    private final ProfileService profilesService;

    private final EventLogger eventLogger = EventLogger.of(ProfileManager.class);

    private final PublishSubject<ProfileAction> actions;

    private final Observable<ProfileResult> results;

    public ProfileManager() {

        profilesService = new ProfileService();

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

        Observable<ProfileResult> profileCreatedResults = actionObservable
                .ofType(CreateProfile.class)
                .map(a -> createMyProfile(a.getPubKey()))
                .map(this::storeMyProfile)
                .flatMap(profile -> profilesService.put(profile).toObservable())
                .map(ProfileCreated::new);

        Observable<ProfileResult> updateProfileResults = actionObservable
                .ofType(UpdateProfile.class)
                .flatMap(a -> loadMyProfile()
                        .map(oldProfile -> updateMyProfile(oldProfile, a.getProfile()))
                        .map(this::storeMyProfile)
                        .flatMap(profile -> profilesService.put(profile).toObservable())
                        .map(ProfileUpdated::new)
                );

        // payment details actions to results

        Observable<ProfileResult> loadPaymentDetailsResults = actionObservable
                .ofType(LoadPaymentDetails.class)
                .flatMap(a -> loadPaymentDetails())
                .map(PaymentDetailsLoaded::new);

        Observable<ProfileResult> updatePaymentDetailsResults = actionObservable
                .ofType(UpdatePaymentDetails.class)
                .map(a -> a.getPaymentDetails())
                .flatMap(this::storePaymentDetails)
                .map(PaymentDetailsUpdated::new);

        // arbitrator profile action results
        Observable<ProfileResult> loadArbitratorProfiles = actionObservable
                .ofType(LoadArbitratorProfiles.class)
                .flatMap(a -> profilesService.get().toObservable())
                .flatMapIterable(l -> l)
                .filter(Profile::isArbitrator)
                .map(ArbitratorProfileLoaded::new);

        results = profileLoadedResults
                .mergeWith(profileNotCreatedResults)
                .mergeWith(profileCreatedResults)
                .mergeWith(updateProfileResults)
                .mergeWith(loadPaymentDetailsResults)
                .mergeWith(updatePaymentDetailsResults)
                .mergeWith(loadArbitratorProfiles)
                .onErrorReturn(e -> new ProfileError(e))
                .startWith(new ProfilePending())
                .compose(eventLogger.logResults())
                .share();
    }

    // my profile

    private Profile updateMyProfile(Profile oldProfile, Profile newProfile) {
        return Profile.builder()
                .pubKey(oldProfile.getPubKey())
                .arbitrator(newProfile.isArbitrator())
                .userName(newProfile.getUserName())
                .phoneNum(newProfile.getPhoneNum())
                .build();
    }

    private Profile storeMyProfile(Profile profile) {

        store(PROFILE_PUBKEY, profile.getPubKey());
        store(PROFILE_ISARBITRATOR, Boolean.valueOf(profile.isArbitrator()).toString());
        store(PROFILE_USERNAME, profile.getUserName());
        store(PROFILE_PHONENUM, profile.getPhoneNum());
        return profile;
    }

    private Profile createMyProfile(String authPubKey) {
        return Profile.builder()
                .pubKey(authPubKey)
                .arbitrator(Boolean.FALSE)
                .build();
    }

    private Observable<Profile> loadMyProfile() {

        return Observable.fromCallable(() -> Profile.builder()
                .pubKey(retrieve(PROFILE_PUBKEY).get())
                .arbitrator(Boolean.valueOf(retrieve(PROFILE_ISARBITRATOR).orElse(Boolean.FALSE.toString())))
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

    public class LoadArbitratorProfiles implements ProfileAction {
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

    public class ArbitratorProfileLoaded implements ProfileResult {
        private final Profile profile;

        public ArbitratorProfileLoaded(Profile profile) {
            this.profile = profile;
        }

        public Profile getProfile() {
            return profile;
        }
    }
}