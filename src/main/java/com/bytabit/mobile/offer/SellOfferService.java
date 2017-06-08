package com.bytabit.mobile.offer;

import com.bytabit.mobile.offer.model.SellOffer;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface SellOfferService {

    @POST("/v1/offers")
    Call<SellOffer> post(@Body SellOffer sellOffer);

    @GET("/v1/offers")
    Call<List<SellOffer>> get();

    @PUT("/v1/offers/{sellerEscrowPubkey}")
    Call<SellOffer> put(@Path("sellerEscrowPubkey") String sellerEscrowPubkey, @Body SellOffer offer);

    @DELETE("/v1/offers/{sellerEscrowPubkey}")
    Call<Void> delete(@Path("sellerEscrowPubkey") String sellerEscrowPubkey);
}
