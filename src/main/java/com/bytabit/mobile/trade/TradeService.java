package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.*;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface TradeService {

    @Headers("Content-Type:application/json")
    @POST("/trades/{escrowAddress}")
    Call<Trade> put(@Path("sellerEscrowPubkey") String sellerEscrowPubKey, @Body BuyRequest buyRequest);

    @Headers("Content-Type:application/json")
    @GET("/trades")
    Call<List<Trade>> get(@Query("profilePubKey") String profilePubKey);

    @Headers("Content-Type:application/json")
    @PUT("/trades/{escrowAddress}/paymentRequest")
    Call<Trade> put(@Path("escrowAddress") String escrowAddress, @Body PaymentRequest paymentRequest);

    @Headers("Content-Type:application/json")
    @PUT("/trades/{escrowAddress}/payoutRequest")
    Call<Trade> put(@Path("escrowAddress") String escrowAddress, @Body PayoutRequest payoutRequest);

    @Headers("Content-Type:application/json")
    @PUT("/trades/{escrowAddress}/arbitrateRequest")
    Call<Trade> put(@Path("escrowAddress") String escrowAddress, @Body ArbitrateRequest arbitrateRequest);

    @Headers("Content-Type:application/json")
    @PUT("/trades/{escrowAddress}/payoutCompleted")
    Call<Trade> put(@Path("escrowAddress") String escrowAddress, @Body PayoutCompleted payoutCompleted);
}
