package com.bytabit.mobile.profile;

import com.bytabit.mobile.profile.model.Profile;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ProfileService {

    @POST("/api/v1/profiles")
    Call<Profile> createProfile(@Body Profile profile);

    @PUT("/api/v1/profiles/{pubkey}")
    Call<Profile> updateProfile(@Path("pubkey") String pubkey, @Body Profile profile);
}
