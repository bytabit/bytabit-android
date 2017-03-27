package com.bytabit.mobile.trade;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.trade.model.Trade;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface TradeService {

    @POST("/v1/trades/{offerPubkey}")
    Call<Trade> create(@Body Trade trade);

    @GET("/v1/trades/{offerPubkey}")
    Call<List<Trade>> read();

    @PUT("/v1/trades/{offerPubkey}/{escrowAddress}")
    Call<SellOffer> update(@Path("offerPubkey") String offerPubkey,
                           @Path("escrowAddress") String escrowAddress,
                           @Body Trade trade);

    @DELETE("/v1/trades/{offerPubkey}/{buyerTradePubKey}")
    Call<SellOffer> delete(@Path("pubkey") String pubkey);
}
