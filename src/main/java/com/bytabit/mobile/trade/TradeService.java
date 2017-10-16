package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.Trade;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface TradeService {

    @Headers("Content-Type:application/json")
    @GET("/trades")
    Call<List<Trade>> get(@Query("profilePubKey") String profilePubKey);

    @Headers("Content-Type:application/json")
    @PUT("/trades/{escrowAddress}")
    Call<Trade> put(@Path("escrowAddress") String escrowAddress, @Body Trade trade);
}
