package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.PayoutRequest;
import retrofit2.Call;
import retrofit2.http.*;

public interface PayoutRequestService {

    @Headers("Content-Type:application/json")
    @POST("/v1/trades/{escrowAddress}/payoutRequest")
    Call<PayoutRequest> post(@Path("escrowAddress") String escrowAddress, @Body PayoutRequest payoutRequest);

    @Headers("Content-Type:application/json")
    @GET("/v1/trades/{escrowAddress}/payoutRequest")
    Call<PayoutRequest> get(@Path("escrowAddress") String escrowAddress);
}
