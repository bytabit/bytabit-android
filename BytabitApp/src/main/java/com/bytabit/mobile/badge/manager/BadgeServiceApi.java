package com.bytabit.mobile.badge.manager;

import com.bytabit.mobile.badge.model.Badge;
import com.bytabit.mobile.badge.model.BadgeRequest;
import io.reactivex.Single;
import retrofit2.http.*;

import java.util.List;

public interface BadgeServiceApi {

    @PUT("/badges/{id}")
    Single<Badge> put(@Path("id") String id, @Body BadgeRequest badgeRequest);

    @GET("/badges")
    Single<List<Badge>> get(@Query("profilePubKey") String profilePubKey);
}
