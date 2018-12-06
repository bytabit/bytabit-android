package com.bytabit.mobile.offer.manager;

import com.bytabit.mobile.offer.model.Offer;
import io.reactivex.Single;
import retrofit2.http.*;

import java.util.List;

public interface OfferServiceApi {

    @PUT("/offers/{traderEscrowPubKey}")
    Single<Offer> put(@Path("traderEscrowPubKey") String sellerEscrowPubKey, @Body Offer sellOffer);

    @GET("/offers")
    Single<List<Offer>> get();

    @DELETE("/offers/{traderEscrowPubKey}")
    Single<Offer> delete(@Path("traderEscrowPubKey") String sellerEscrowPubKey);
}
