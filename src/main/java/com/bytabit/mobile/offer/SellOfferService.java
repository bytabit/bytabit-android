package com.bytabit.mobile.offer;

import com.bytabit.mobile.offer.model.SellOffer;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface SellOfferService {

    @Headers("Content-Type:application/json")
    @POST("/v1/offers")
    Call<SellOffer> post(@Body SellOffer sellOffer);

    @Headers("Content-Type:application/json")
    @GET("/v1/offers")
    Call<List<SellOffer>> get();

    @Headers("Content-Type:application/json")
    @PUT("/v1/offers/{sellerEscrowPubkey}")
    Call<SellOffer> put(@Path("sellerEscrowPubkey") String sellerEscrowPubkey, @Body SellOffer offer);

    @Headers("Content-Type:application/json")
    @DELETE("/v1/offers/{sellerEscrowPubkey}")
    Call<Void> delete(@Path("sellerEscrowPubkey") String sellerEscrowPubkey);
}
