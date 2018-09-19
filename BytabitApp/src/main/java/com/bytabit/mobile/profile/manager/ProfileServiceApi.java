package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.profile.model.Profile;
import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import java.util.List;

public interface ProfileServiceApi {

    @PUT("/profiles/{profilePubKey}")
    Single<Profile> put(@Path("profilePubKey") String profilePubKey, @Body Profile profile);

    @GET("/profiles")
    Single<List<Profile>> get();
}
