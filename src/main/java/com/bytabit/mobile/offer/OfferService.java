package com.bytabit.mobile.offer;

import com.bytabit.mobile.offer.model.Offer;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface OfferService {

    @POST("/v1/offers")
    Call<Offer> createOffer(@Body Offer offer);

    @GET("/v1/offers")
    Call<List<Offer>> readOffers();

    @PUT("/v1/offers/{pubkey}")
    Call<Offer> updateOffer(@Path("pubkey") String pubkey, @Body Offer offer);
}
