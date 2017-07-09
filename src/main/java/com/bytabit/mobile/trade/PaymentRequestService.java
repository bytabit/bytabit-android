package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.PaymentRequest;
import retrofit2.Call;
import retrofit2.http.*;

public interface PaymentRequestService {

    @Headers("Content-Type:application/json")
    @POST("/v1/trades/{escrowAddress}/paymentRequest")
    Call<PaymentRequest> post(@Path("escrowAddress") String escrowAddress, @Body PaymentRequest paymentRequest);

    @Headers("Content-Type:application/json")
    @GET("/v1/trades/{escrowAddress}/paymentRequest")
    Call<PaymentRequest> get(@Path("escrowAddress") String escrowAddress);
}
