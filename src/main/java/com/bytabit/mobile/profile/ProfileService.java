package com.bytabit.mobile.profile;

import com.bytabit.mobile.profile.model.Profile;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface ProfileService {

    @Headers("Content-Type:application/json")
    @PUT("/profiles/{pubKey}")
    Call<Profile> put(@Path("pubKey") String pubkey, @Body Profile profile);

    @Headers("Content-Type:application/json")
    @GET("/profiles")
    Call<List<Profile>> get();
}
