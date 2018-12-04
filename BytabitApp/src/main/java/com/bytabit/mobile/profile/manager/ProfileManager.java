package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.StorageManager;
import com.bytabit.mobile.profile.model.Profile;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class ProfileManager {

    private static final String PROFILE_USERNAME = "profile.name";
    private static final String PROFILE_PHONENUM = "profile.phoneNum";

    @Inject
    private StorageManager storageManager;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PublishSubject<Profile> updatedProfile = PublishSubject.create();

    // my profile

    public Profile storeMyProfile(Profile profile) {
        storageManager.store(PROFILE_USERNAME, profile.getUserName());
        storageManager.store(PROFILE_PHONENUM, profile.getPhoneNum());
        updatedProfile.onNext(profile);
        return profile;
    }

    public Profile retrieveMyProfile() {
        return Profile.builder()
                .userName(storageManager.retrieve(PROFILE_USERNAME).orElse(""))
                .phoneNum(storageManager.retrieve(PROFILE_PHONENUM).orElse(""))
                .build();
    }

    public Observable<Profile> getUpdatedProfile() {
        return updatedProfile
                .startWith(retrieveMyProfile())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnNext(p -> log.debug("Updated Profile: {}", p))
                .share();
    }
}