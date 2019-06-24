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

package com.bytabit.app.core.offer.manager;

import com.bytabit.app.core.common.AppConfig;
import com.bytabit.app.core.common.net.RetrofitService;
import com.bytabit.app.core.offer.model.Offer;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class OfferService extends RetrofitService {

    private final OfferServiceApi offerServiceApi;

    @Inject
    public OfferService(AppConfig appConfig) {
        super(appConfig);

        // create an instance of the ApiService
        offerServiceApi = retrofit
                .create(OfferServiceApi.class);
    }

    Single<Offer> put(Offer offer) {
        Single<Offer> putOffer = offerServiceApi.put(offer.getId(), offer);
        return putOffer
                .doOnError(t -> log.error("put error: {}", t.getMessage()));
    }

    Single<List<Offer>> getAll() {
        return offerServiceApi.get()
                .doOnError(t -> log.error("get error: {}", t.getMessage()));
    }

    Single<Offer> get(String id) {
        return offerServiceApi.get(id)
                .doOnError(t -> log.error("get error: {}", t.getMessage()));
    }

    Single<Offer> delete(String id) {

        return offerServiceApi.delete(id)
                .doOnError(t -> log.error("delete error: {}", t.getMessage()));
    }
}
