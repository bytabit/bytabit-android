/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.app.core.trade.manager;

import com.bytabit.app.core.common.AppConfig;
import com.bytabit.app.core.common.RetryWithDelay;
import com.bytabit.app.core.common.net.RetrofitService;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.core.trade.model.TradeServiceResource;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class TradeService extends RetrofitService {

    private final TradeServiceApi tradeServiceApi;

    @Inject
    public TradeService(AppConfig appConfig) {
        super(appConfig);

        // create an instance of the ApiService
        this.tradeServiceApi = retrofit.create(TradeServiceApi.class);
    }

    Single<Trade> put(Trade trade) {

        return tradeServiceApi.put(trade.getId(), TradeServiceResource.fromTrade(trade))
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("put error: {}", t.getMessage()))
                .map(tr -> TradeServiceResource.toTrade(tr, trade))
                .doOnSuccess(rt -> {
                    // validate sent and received trades are equal except for version, which must be higher
                    if (!rt.equals(trade)) {
                        throw new TradeException("Received trade from put that isn't equal to trade sent.");
                    }
                    if (rt.getVersion() <= trade.getVersion()) {
                        throw new TradeException("Received trade version less than or equal to trade sent.");
                    }
                });
    }

    Single<List<Trade>> getByOfferId(String offerId, Long version) {

        return tradeServiceApi.getByOfferId(offerId, version)
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("get error: {}", t.getMessage()))
                .flatMap(l -> Observable.fromIterable(l).map(TradeServiceResource::toTrade).toList());
    }

    Single<List<Trade>> get(String id, Long version) {

        return tradeServiceApi.get(id, version)
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("get error: {}", t.getMessage()))
                .flatMap(l -> Observable.fromIterable(l).map(TradeServiceResource::toTrade).toList());
    }

    Single<List<Trade>> get(Set<String> ids, Long version) {

        return Observable.fromIterable(ids).flatMap(id -> tradeServiceApi.get(id, version)
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("get error: {}", t.getMessage()))
                .flatMapObservable(l -> Observable.fromIterable(l).map(TradeServiceResource::toTrade)))
                .toList();
    }

    Single<List<Trade>> getArbitrate(Long version) {
        return tradeServiceApi.getArbitrate(version)
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("get error: {}", t.getMessage()))
                .flatMap(l -> Observable.fromIterable(l).map(TradeServiceResource::toTrade).toList());
    }
}
