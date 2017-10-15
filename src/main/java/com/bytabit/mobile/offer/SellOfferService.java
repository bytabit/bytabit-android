package com.bytabit.mobile.offer;

import com.bytabit.mobile.offer.model.SellOffer;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface SellOfferService {

    @Headers("Content-Type:application/json")
    @PUT("/offers/{sellerEscrowPubKey}")
    Call<SellOffer> put(@Path("sellerEscrowPubKey") String sellerEscrowPubKey, @Body SellOffer sellOffer);

    @Headers("Content-Type:application/json")
    @GET("/offers")
    Call<List<SellOffer>> get();

    @Headers("Content-Type:application/json")
    @DELETE("/offers/{sellerEscrowPubKey}")
    Call<Void> delete(@Path("sellerEscrowPubKey") String sellerEscrowPubKey);
}
