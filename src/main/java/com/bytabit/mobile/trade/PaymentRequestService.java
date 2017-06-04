package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.PaymentRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface PaymentRequestService {

    @POST("/v1/trades/{escrowAddress}/paymentRequest")
    Call<PaymentRequest> createPaymentRequest(@Path("escrowAddress") String escrowAddress, @Body PaymentRequest paymentRequest);

    @GET("/v1/trades/{escrowAddress}/paymentRequest")
    Call<PaymentRequest> readPaymentRequest(@Path("escrowAddress") String escrowAddress);
}
