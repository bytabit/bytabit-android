package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.RetrofitService;
import com.bytabit.mobile.profile.model.Profile;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ProfileService extends RetrofitService {

    private final ProfileServiceApi profileServiceApi;

    public ProfileService() {

        // create an instance of the ApiService
        profileServiceApi = retrofit.create(ProfileServiceApi.class);
    }

    Single<Profile> put(Profile profile) {

        return profileServiceApi.put(profile.getPubKey(), profile)
                .doOnError(t -> log.error("put error: {}", t.getMessage()));
    }

    Single<List<Profile>> get() {

        return profileServiceApi.get()
                .doOnError(t -> log.error("get error: {}", t.getMessage()));
    }
}
