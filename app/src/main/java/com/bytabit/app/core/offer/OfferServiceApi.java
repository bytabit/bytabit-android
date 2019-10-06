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

package com.bytabit.app.core.offer;

import com.bytabit.app.core.offer.model.SignedOffer;

import java.util.List;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface OfferServiceApi {

    @PUT("/offers/{id}")
    Single<SignedOffer> put(@Path("id") String id, @Body SignedOffer signedOffer);

    @GET("/offers")
    Single<List<SignedOffer>> get();

    @GET("/offers/{id}")
    Single<SignedOffer> get(@Path("id") String id);

    @DELETE("/offers/{id}")
    Single<SignedOffer> delete(@Path("id") String id);
}
