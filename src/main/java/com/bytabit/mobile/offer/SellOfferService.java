package com.bytabit.mobile.offer;

import com.bytabit.mobile.offer.model.SellOffer;
import io.reactivex.Completable;
import io.reactivex.Single;
import retrofit2.http.*;

import java.util.List;

public interface SellOfferService {

    @Headers("Content-Type:application/json")
    @PUT("/offers/{sellerEscrowPubKey}")
    Single<SellOffer> put(@Path("sellerEscrowPubKey") String sellerEscrowPubKey, @Body SellOffer sellOffer);

    @Headers("Content-Type:application/json")
    @GET("/offers")
    Single<List<SellOffer>> get();

    @Headers("Content-Type:application/json")
    @DELETE("/offers/{sellerEscrowPubKey}")
    Completable delete(@Path("sellerEscrowPubKey") String sellerEscrowPubKey);
}
