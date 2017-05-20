package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.PayoutRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface PayoutRequestService {

    @POST("/v1/trades/{escrowAddress}/payoutRequest")
    Call<PayoutRequest> createPayoutRequest(@Path("escrowAddress") String escrowAddress, @Body PayoutRequest payoutRequest);

    @GET("/v1/trades/{escrowAddress}/payoutRequest")
    Call<PayoutRequest> readPayoutRequests(@Path("escrowAddress") String escrowAddress);
}
