/*
 * Copyright 2018 Bytabit AB
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

package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.common.RetrofitService;
import com.bytabit.mobile.common.RetryWithDelay;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.trade.model.TradeManagerException;
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

    Single<List<Trade>> get(String profilePubKey, Long version) {

        return tradeServiceApi.get(profilePubKey, version)
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("get error: {}", t.getMessage()))
                .flatMap(l -> Observable.fromIterable(l).map(TradeServiceResource::toTrade).toList());
    }

    Single<Trade> put(Trade trade) {

        return tradeServiceApi.put(trade.getId(), TradeServiceResource.fromTrade(trade))
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("put error: {}", t.getMessage()))
                .map(tr -> TradeServiceResource.toTrade(tr, trade))
                .doOnSuccess(rt -> {
                    // validate sent and received trades are equal except for version, which must be higher
                    if (!rt.equals(trade)) {
                        throw new TradeManagerException("Received trade from put that isn't equal to trade sent.");
                    }
                    if (rt.getVersion() <= trade.getVersion()) {
                        throw new TradeManagerException("Received trade version less than or equal to trade sent.");
                    }
                });
    }
}
