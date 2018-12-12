package com.bytabit.mobile.offer.manager;

import com.bytabit.mobile.offer.model.Offer;
import io.reactivex.Single;
import retrofit2.http.*;

import java.util.List;

public interface OfferServiceApi {

    @PUT("/offers/{id}")
    Single<Offer> put(@Path("id") String id, @Body Offer sellOffer);

    @GET("/offers")
    Single<List<Offer>> get();

    @DELETE("/offers/{id}")
    Single<Offer> delete(@Path("id") String id);
}
