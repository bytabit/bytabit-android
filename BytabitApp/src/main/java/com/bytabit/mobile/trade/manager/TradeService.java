package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.common.RetryWithDelay;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.trade.model.TradeServiceResource;
import com.gluonhq.connect.provider.ListDataReader;
import com.gluonhq.connect.provider.ObjectDataWriter;
import com.gluonhq.connect.provider.RestClient;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TradeService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    Single<List<Trade>> get(String profilePubKey) {

        return Single.<List<Trade>>create(source -> {

            RestClient getRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path("/trades")
                    .queryParam("profilePubKey", profilePubKey)
                    .method("GET")
                    .contentType("application/json");

            ListDataReader<TradeServiceResource> listDataReader = getRestClient.createListDataReader(TradeServiceResource.class);

            try {
                List<Trade> trades = new ArrayList<>();
                Iterator<TradeServiceResource> tradeIterator = listDataReader.iterator();
                while (tradeIterator.hasNext()) {
                    trades.add(TradeServiceResource.toTrade(tradeIterator.next()));
                }
                source.onSuccess(trades);
            } catch (IOException ioe) {
                source.onError(ioe);
            }
        })
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("get error: {}", t.getMessage()));
    }

    Single<Trade> put(Trade trade) {

        return Single.<Trade>create(source -> {

            RestClient putRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path(String.format("/trades/%s", trade.getEscrowAddress()))
                    .method("PUT")
                    .contentType("application/json");

            ObjectDataWriter<TradeServiceResource> dataWriter = putRestClient.createObjectDataWriter(TradeServiceResource.class);

            try {
                // write trade and return original trade object
                dataWriter.writeObject(TradeServiceResource.fromTrade(trade)).ifPresent(wt -> source.onSuccess(trade));
            } catch (Exception e) {
                source.onError(e);
            }
        })
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("put error: {}", t.getMessage()));
    }
}
