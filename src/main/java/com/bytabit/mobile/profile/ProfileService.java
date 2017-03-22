package com.bytabit.mobile.profile;

import com.bytabit.mobile.profile.model.Profile;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface ProfileService {

    @POST("/v1/profiles")
    Call<Profile> create(@Body Profile profile);

    @GET("/v1/profiles")
    Call<List<Profile>> read();

    @PUT("/v1/profiles/{pubkey}")
    Call<Profile> update(@Path("pubkey") String pubkey, @Body Profile profile);
}
