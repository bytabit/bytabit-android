package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.StorageManager;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.wallet.manager.WalletManager;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Optional;

public class ProfileManager {

    private static final String PROFILE_PUBKEY = "profile.pubkey";
    private static final String PROFILE_ISARBITRATOR = "profile.arbitrator";
    private static final String PROFILE_USERNAME = "profile.name";
    private static final String PROFILE_PHONENUM = "profile.phoneNum";

    @Inject
    private StorageManager storageManager;

    @Inject
    private WalletManager walletManager;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PublishSubject<Profile> updatedProfile = PublishSubject.create();

    @PostConstruct
    public void initialize() {

        walletManager.getEscrowWalletConfig()
                .filter(c -> c.getMnemonicCode() != null || c.getCreationDate() != null)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(c -> clearProfilePubKey());
    }

    // my profile

    public void updateMyProfile(Profile newProfile) {
        loadOrCreateMyProfile().map(oldProfile -> Profile.builder()
                .pubKey(oldProfile.getPubKey())
                .isArbitrator(newProfile.isArbitrator())
                .userName(newProfile.getUserName())
                .phoneNum(newProfile.getPhoneNum())
                .build())
                .map(this::storeMyProfile)
                .subscribe(updatedProfile::onNext);
    }

    private Profile storeMyProfile(Profile profile) {

        storageManager.store(PROFILE_ISARBITRATOR, Boolean.toString(profile.isArbitrator()));
        storageManager.store(PROFILE_USERNAME, profile.getUserName());
        storageManager.store(PROFILE_PHONENUM, profile.getPhoneNum());
        return profile;
    }

    public void clearProfilePubKey() {
        storageManager.remove(PROFILE_PUBKEY);
    }

    public Maybe<Profile> loadOrCreateMyProfile() {
        Optional<String> profilePubKey = storageManager.retrieve(PROFILE_PUBKEY);

        if (profilePubKey.isPresent()) {
            return Maybe.just(profilePubKey.get())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .map(this::getProfile)
                    .doOnSuccess(p -> log.debug("Load Profile: {}", p));
        } else {
            return walletManager.getTradeWalletProfilePubKey()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .doOnSuccess(pubKey -> storageManager.store(PROFILE_PUBKEY, pubKey))
                    .map(this::getProfile)
                    .doOnSuccess(p -> log.debug("Create Profile: {}", p));
        }
    }

    private Profile getProfile(String profilePubKey) {
        return Profile.builder()
                .pubKey(profilePubKey)
                .isArbitrator(Boolean.valueOf(storageManager.retrieve(PROFILE_ISARBITRATOR).orElse(Boolean.FALSE.toString())))
                .userName(storageManager.retrieve(PROFILE_USERNAME).orElse(""))
                .phoneNum(storageManager.retrieve(PROFILE_PHONENUM).orElse(""))
                .build();
    }

    public Observable<Profile> getUpdatedProfile() {
        return updatedProfile
                .startWith(loadOrCreateMyProfile().toObservable())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnNext(p -> log.debug("Updated Profile: {}", p))
                .share();
    }
}