package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.Trade;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

public interface TradeService {

    @Headers("Content-Type:application/json")
    @GET("/v1/trades/{escrowAddress}")
    Call<Trade> post(@Path("escrowAddress") String escrowAddress);

    @Headers("Content-Type:application/json")
    @GET("/v1/trades")
    Call<List<Trade>> get(@Query("arbitratorProfilePubKey") String arbitratorProfilePubKey);
}
