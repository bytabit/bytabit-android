package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.PayoutCompleted;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface PayoutCompletedService {

    @POST("/v1/trades/{escrowAddress}/payoutCompleted")
    Call<PayoutCompleted> post(@Path("escrowAddress") String escrowAddress, @Body PayoutCompleted payoutCompleted);

    @GET("/v1/trades/{escrowAddress}/payoutCompleted")
    Call<PayoutCompleted> get(@Path("escrowAddress") String escrowAddress);
}
