package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.PayoutCompleted;
import retrofit2.Call;
import retrofit2.http.*;

public interface PayoutCompletedService {

    @Headers("Content-Type:application/json")
    @POST("/v1/trades/{escrowAddress}/payoutCompleted")
    Call<PayoutCompleted> post(@Path("escrowAddress") String escrowAddress, @Body PayoutCompleted payoutCompleted);

    @Headers("Content-Type:application/json")
    @GET("/v1/trades/{escrowAddress}/payoutCompleted")
    Call<PayoutCompleted> get(@Path("escrowAddress") String escrowAddress);
}
