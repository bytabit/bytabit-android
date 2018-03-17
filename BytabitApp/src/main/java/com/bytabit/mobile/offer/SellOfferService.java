package com.bytabit.mobile.offer;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.gluonhq.connect.provider.ListDataReader;
import com.gluonhq.connect.provider.ObjectDataReader;
import com.gluonhq.connect.provider.ObjectDataWriter;
import com.gluonhq.connect.provider.RestClient;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SellOfferService {

//    @Headers("Content-Type:application/json")
//    @PUT("/offers/{sellerEscrowPubKey}")
//    Single<SellOffer> put(@Path("sellerEscrowPubKey") String sellerEscrowPubKey, @Body SellOffer sellOffer);
//
//    @Headers("Content-Type:application/json")
//    @GET("/offers")
//    Single<List<SellOffer>> get();
//
//    @Headers("Content-Type:application/json")
//    @DELETE("/offers/{sellerEscrowPubKey}")
//    Completable delete(@Path("sellerEscrowPubKey") String sellerEscrowPubKey);

    Single<SellOffer> putOffer(SellOffer sellOffer) {
        return Single.create((SingleEmitter<SellOffer> source) -> {

            RestClient putRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path(String.format("/offers/%s", sellOffer.getSellerEscrowPubKey()))
                    .method("PUT")
                    .contentType("application/json");

            ObjectDataWriter<SellOffer> dataWriter = putRestClient.createObjectDataWriter(SellOffer.class);

            try {
                dataWriter.writeObject(sellOffer).ifPresent(source::onSuccess);
            } catch (Exception e) {
                source.onError(e);
            }
        });
    }

    Single<List<SellOffer>> getOffers() {

        return Single.create(source -> {

            RestClient getRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path("/offers")
                    .method("GET")
                    .contentType("application/json");

            ListDataReader<SellOffer> listDataReader = getRestClient.createListDataReader(SellOffer.class);

            try {
                List<SellOffer> sellOffers = new ArrayList<>();
                listDataReader.iterator().forEachRemaining(sellOffers::add);
                source.onSuccess(sellOffers);
            } catch (IOException ioe) {
                source.onError(ioe);
            }
        });
    }

    Single<SellOffer> deleteOffer(String sellerEscrowPubKey) {

        return Single.create(source -> {

            RestClient deleteRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path(String.format("/offers/%s", sellerEscrowPubKey))
                    .method("DELETE")
                    .contentType("application/json");

            ObjectDataReader<SellOffer> dataReader = deleteRestClient.createObjectDataReader(SellOffer.class);

            try {
                dataReader.readObject();
                source.onSuccess(SellOffer.builder().sellerEscrowPubKey(sellerEscrowPubKey).build());
            } catch (IOException ioe) {
                source.onError(ioe);
            }
        });
    }
}
