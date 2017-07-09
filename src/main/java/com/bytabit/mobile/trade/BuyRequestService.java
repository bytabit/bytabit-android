package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.BuyRequest;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface BuyRequestService {

    @Headers("Content-Type:application/json")
    @POST("/v1/offers/{sellerEscrowPubkey}/buyRequests")
    Call<BuyRequest> post(@Path("sellerEscrowPubkey") String sellerEscrowPubKey, @Body BuyRequest buyRequest);

    @Headers("Content-Type:application/json")
    @GET("/v1/offers/{sellerEscrowPubkey}/buyRequests")
    Call<List<BuyRequest>> get(@Path("sellerEscrowPubkey") String sellerEscrowPubkey);
}
