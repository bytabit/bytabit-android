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

package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.trade.model.TradeStorageResource;
import io.reactivex.Maybe;
import io.reactivex.Single;

import java.util.List;

public class TradeStorage {

    private final TradeResourceStorage tradeResourceStorage = new TradeResourceStorage();

    public Single<Trade> write(Trade trade) {
        return tradeResourceStorage.write(TradeStorageResource.fromTrade(trade))
                .map(TradeStorageResource::toTrade);
    }

    public Maybe<Trade> read(String id) {
        return tradeResourceStorage.read(id)
                .map(TradeStorageResource::toTrade);
    }

    public Single<List<Trade>> getAll() {
        return tradeResourceStorage.getAll().flattenAsObservable(trl -> trl)
                .map(TradeStorageResource::toTrade)
                .toList();
    }
}
