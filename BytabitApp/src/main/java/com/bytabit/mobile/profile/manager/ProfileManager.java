package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.wallet.manager.WalletManager;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class ProfileManager extends AbstractManager {

    private String PROFILE_PUBKEY = "profile.pubkey";
    private String PROFILE_ISARBITRATOR = "profile.arbitrator";
    private String PROFILE_USERNAME = "profile.name";
    private String PROFILE_PHONENUM = "profile.phoneNum";

    private final ProfileService profilesService;

    @Inject
    private WalletManager walletManager;

    private final EventLogger eventLogger = EventLogger.of(ProfileManager.class);

    private final PublishSubject<Profile> updatedProfile = PublishSubject.create();

    public ProfileManager() {
        profilesService = new ProfileService();
    }

    // my profile

    public void updateMyProfile(Profile newProfile) {
        loadMyProfile().map(oldProfile -> Profile.builder()
                .pubKey(oldProfile.getPubKey())
                .arbitrator(newProfile.isArbitrator())
                .userName(newProfile.getUserName())
                .phoneNum(newProfile.getPhoneNum())
                .build())
                .map(this::storeMyProfile)
                .flatMap(profile -> profilesService.put(profile).toObservable())
                .subscribe(updatedProfile::onNext);
    }

    private Profile storeMyProfile(Profile profile) {

        store(PROFILE_PUBKEY, profile.getPubKey());
        store(PROFILE_ISARBITRATOR, Boolean.valueOf(profile.isArbitrator()).toString());
        store(PROFILE_USERNAME, profile.getUserName());
        store(PROFILE_PHONENUM, profile.getPhoneNum());
        return profile;
    }

    public Observable<Profile> loadMyProfile() {
        Optional<String> profilePubKey = retrieve(PROFILE_PUBKEY);

        if (profilePubKey.isPresent()) {
            return Observable.just(profilePubKey.get())
                    .map(this::getProfile);
        } else {
            return walletManager.getTradeWalletProfilePubKey()
                    .map(this::getProfile);
        }
    }

    public Observable<List<Profile>> loadArbitratorProfiles() {
        // TODO refresh values, cache latest results
        return profilesService.get().toObservable();
    }


    private Profile getProfile(String profilePubKey) {
        return Profile.builder()
                .pubKey(profilePubKey)
                .arbitrator(Boolean.valueOf(retrieve(PROFILE_ISARBITRATOR).orElse(Boolean.FALSE.toString())))
                .userName(retrieve(PROFILE_USERNAME).orElse(""))
                .phoneNum(retrieve(PROFILE_PHONENUM).orElse(""))
                .build();
    }

    public Observable<Profile> getUpdatedProfile() {
        return updatedProfile
                .compose(eventLogger.logObjects("UpdatedProfile"))
                .share();
    }
}