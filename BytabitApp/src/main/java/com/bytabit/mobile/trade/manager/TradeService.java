package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.common.RetrofitService;
import com.bytabit.mobile.common.RetryWithDelay;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.trade.model.TradeServiceResource;
import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TradeService extends RetrofitService {

    private final TradeServiceApi tradeServiceApi;

    public TradeService() {

        // create an instance of the ApiService
        this.tradeServiceApi = retrofit.create(TradeServiceApi.class);
    }

    Single<List<Trade>> get(String profilePubKey) {

        return tradeServiceApi.get(profilePubKey)
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("get error: {}", t.getMessage()))
                .flatMap(l -> Observable.fromIterable(l).map(TradeServiceResource::toTrade).toList());
    }

    Single<Trade> put(Trade trade) {

        return tradeServiceApi.put(trade.getEscrowAddress(), TradeServiceResource.fromTrade(trade))
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("put error: {}", t.getMessage()))
                .map(TradeServiceResource::toTrade);
    }
}
