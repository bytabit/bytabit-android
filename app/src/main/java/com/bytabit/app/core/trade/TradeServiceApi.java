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

package com.bytabit.app.core.trade;

import com.bytabit.app.core.trade.model.TradeServiceResource;

import java.util.List;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TradeServiceApi {

    @PUT("/trades/{id}")
    Single<TradeServiceResource> put(@Path("id") String id, @Body TradeServiceResource trade);

    @GET("/offers/{offerId}/trades")
    Single<List<TradeServiceResource>> getByOfferId(@Path("offerId") String offerId, @Query("version") Long version);

    @GET("/trades/{id}")
    Single<List<TradeServiceResource>> get(@Path("id") String id, @Query("version") Long version);

    @GET("/trades/arbitrate")
    Single<List<TradeServiceResource>> getArbitrate(@Query("version") Long version);
}
