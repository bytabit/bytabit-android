package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.trade.model.Trade;
import com.gluonhq.connect.provider.ListDataReader;
import com.gluonhq.connect.provider.ObjectDataWriter;
import com.gluonhq.connect.provider.RestClient;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TradeService {

//    @Headers("Content-Type:application/json")
//    @GET("/trades")
//    Single<List<Trade>> get(@Query("profilePubKey") String profilePubKey);
//
//    @Headers("Content-Type:application/json")
//    @PUT("/trades/{escrowAddress}")
//    Single<Trade> put(@Path("escrowAddress") String escrowAddress, @Body Trade trade);

    public Single<List<Trade>> get(String profilePubKey) {

        return Single.create(source -> {

            RestClient getRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path("/trades")
                    .queryParam("profilePubKey", profilePubKey)
                    .method("GET")
                    .contentType("application/json");

            ListDataReader<Trade> listDataReader = getRestClient.createListDataReader(Trade.class);

            try {
                List<Trade> trades = new ArrayList<>();
                Iterator<Trade> tradeIterator = listDataReader.iterator();
                while (tradeIterator.hasNext()) {
                    trades.add(tradeIterator.next());
                }
                source.onSuccess(trades);
            } catch (IOException ioe) {
                source.onError(ioe);
            }
        });
    }

    public Single<Trade> put(Trade trade) {

        return Single.create((SingleEmitter<Trade> source) -> {

            RestClient putRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path(String.format("/trades/%s", trade.getEscrowAddress()))
                    .method("PUT")
                    .contentType("application/json");

            ObjectDataWriter<Trade> dataWriter = putRestClient.createObjectDataWriter(Trade.class);

            try {
                dataWriter.writeObject(trade).ifPresent(source::onSuccess);
            } catch (Exception e) {
                source.onError(e);
            }
        });
    }
}
