package com.bytabit.mobile.offer.manager;

import com.bytabit.mobile.offer.model.SellOffer;
import io.reactivex.Single;
import retrofit2.http.*;

import java.util.List;

public interface SellOfferServiceApi {

    @PUT("/offers/{sellerEscrowPubKey}")
    Single<SellOffer> put(@Path("sellerEscrowPubKey") String sellerEscrowPubKey, @Body SellOffer sellOffer);

    @GET("/offers")
    Single<List<SellOffer>> get();

    @DELETE("/offers/{sellerEscrowPubKey}")
    Single<SellOffer> delete(@Path("sellerEscrowPubKey") String sellerEscrowPubKey);
}
