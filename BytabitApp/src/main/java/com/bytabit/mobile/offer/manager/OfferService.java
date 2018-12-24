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

import com.bytabit.mobile.common.RetrofitService;
import com.bytabit.mobile.offer.model.Offer;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class OfferService extends RetrofitService {

    private final OfferServiceApi offerServiceApi;

    public OfferService() {

        // create an instance of the ApiService
        offerServiceApi = retrofit.create(OfferServiceApi.class);
    }

    Single<Offer> put(Offer sellOffer) {
        return offerServiceApi.put(sellOffer.getId(), sellOffer)
                .doOnError(t -> log.error("put error: {}", t.getMessage()));
    }

    Single<List<Offer>> getAll() {
        return offerServiceApi.get()
                .doOnError(t -> log.error("get error: {}", t.getMessage()));
    }

    Single<Offer> delete(String id) {

        return offerServiceApi.delete(id)
                .doOnError(t -> log.error("delete error: {}", t.getMessage()));
    }
}
