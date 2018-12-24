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

package com.bytabit.mobile.offer.manager;

import com.bytabit.mobile.offer.model.Offer;
import io.reactivex.Single;
import retrofit2.http.*;

import java.util.List;

public interface OfferServiceApi {

    @PUT("/offers/{id}")
    Single<Offer> put(@Path("id") String id, @Body Offer sellOffer);

    @GET("/offers")
    Single<List<Offer>> get();

    @DELETE("/offers/{id}")
    Single<Offer> delete(@Path("id") String id);
}
