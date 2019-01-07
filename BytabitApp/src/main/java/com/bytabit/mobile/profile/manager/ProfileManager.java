/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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