package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.Trade;
import io.reactivex.Single;
import retrofit2.http.*;

import java.util.List;

public interface TradeService {

    @Headers("Content-Type:application/json")
    @GET("/trades")
    Single<List<Trade>> get(@Query("profilePubKey") String profilePubKey);

    @Headers("Content-Type:application/json")
    @PUT("/trades/{escrowAddress}")
    Single<Trade> put(@Path("escrowAddress") String escrowAddress, @Body Trade trade);
}
