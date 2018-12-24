package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.trade.model.TradeServiceResource;
import io.reactivex.Single;
import retrofit2.http.*;

import java.util.List;

public interface TradeServiceApi {

    @GET("/trades")
    Single<List<TradeServiceResource>> get(@Query("profilePubKey") String profilePubKey, @Query("version") Long version);

    @PUT("/trades/{id}")
    Single<TradeServiceResource> put(@Path("id") String id, @Body TradeServiceResource trade);
}
