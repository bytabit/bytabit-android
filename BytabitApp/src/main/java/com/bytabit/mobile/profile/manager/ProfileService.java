package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.profile.model.Profile;
import io.reactivex.Single;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;

public class ProfileService {

    private final Retrofit retrofit;
    private final ProfileServiceApi profileServiceApi;

    public ProfileService() {
        retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        // create an instance of the ApiService
        profileServiceApi = retrofit.create(ProfileServiceApi.class);
    }

    Single<Profile> put(Profile profile) {

        return profileServiceApi.put(profile.getPubKey(), profile);
    }

    Single<List<Profile>> get() {

        return profileServiceApi.get();
    }
}
