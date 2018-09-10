package com.bytabit.mobile.offer.manager;

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
import java.util.Iterator;
import java.util.List;

public class SellOfferService {

    private static final String APPLICATION_JSON = "application/json";

    Single<SellOffer> put(SellOffer sellOffer) {
        return Single.create((SingleEmitter<SellOffer> source) -> {

            RestClient putRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path(String.format("/offers/%s", sellOffer.getSellerEscrowPubKey()))
                    .method("PUT")
                    .contentType(APPLICATION_JSON);

            ObjectDataWriter<SellOffer> dataWriter = putRestClient.createObjectDataWriter(SellOffer.class);

            try {
                dataWriter.writeObject(sellOffer).ifPresent(source::onSuccess);
            } catch (Exception e) {
                source.onError(e);
            }
        });
    }

    Single<List<SellOffer>> getAll() {

        return Single.create(source -> {

            RestClient getRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path("/offers")
                    .method("GET")
                    .contentType(APPLICATION_JSON);

            ListDataReader<SellOffer> listDataReader = getRestClient.createListDataReader(SellOffer.class);

            try {
                List<SellOffer> sellOffers = new ArrayList<>();
                Iterator<SellOffer> sellOfferIterator = listDataReader.iterator();
                while (sellOfferIterator.hasNext()) {
                    sellOffers.add(sellOfferIterator.next());
                }
                source.onSuccess(sellOffers);
            } catch (IOException ioe) {
                source.onError(ioe);
            }
        });
    }

    Single<SellOffer> delete(String sellerEscrowPubKey) {

        return Single.create(source -> {

            RestClient deleteRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path(String.format("/offers/%s", sellerEscrowPubKey))
                    .method("DELETE")
                    .contentType(APPLICATION_JSON);

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
