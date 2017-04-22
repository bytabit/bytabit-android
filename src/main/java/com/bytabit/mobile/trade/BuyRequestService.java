package com.bytabit.mobile.trade;

import com.bytabit.mobile.offer.model.BuyRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface BuyRequestService {

    @POST("/v1/offers/{sellerEscrowPubkey}/buyRequests")
    Call<BuyRequest> createBuyRequest(@Path("sellerEscrowPubkey") String sellerEscrowPubkey, @Body BuyRequest buyRequest);
}
