package com.bytabit.mobile.profile;

import com.bytabit.mobile.profile.model.Profile;
import io.reactivex.Single;
import retrofit2.http.*;

import java.util.List;

public interface ProfileService {

    @Headers("Content-Type:application/json")
    @PUT("/profiles/{pubKey}")
    Single<Profile> putProfile(@Path("pubKey") String pubkey, @Body Profile profile);

    @Headers("Content-Type:application/json")
    @GET("/profiles")
    Single<List<Profile>> getProfiles();
}
