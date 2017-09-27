package com.bytabit.mobile.offer;

import com.bytabit.mobile.offer.model.SellOffer;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface SellOfferService {

    @Headers("Content-Type:application/json")
    @POST("/offers")
    Call<SellOffer> post(@Body SellOffer sellOffer);

    @Headers("Content-Type:application/json")
    @GET("/offers")
    Call<List<SellOffer>> get();

    @Headers("Content-Type:application/json")
    @DELETE("/offers/{sellerEscrowPubkey}")
    Call<Void> delete(@Path("sellerEscrowPubkey") String sellerEscrowPubkey);
}
