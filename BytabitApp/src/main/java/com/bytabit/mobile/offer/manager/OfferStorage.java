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

package com.bytabit.mobile.offer.manager;

import com.bytabit.mobile.common.DateConverter;
import com.bytabit.mobile.common.RetryWithDelay;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.Offer;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfferStorage {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String OFFERS_PATH = AppConfig.getPrivateStorage().getPath() + File.separator +
            "offers" + File.separator;

    private static final String JSON_EXT = "json";

    private final Gson gson;

    private final File offersDir;

    OfferStorage() {

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Date.class, new DateConverter())
                .create();

        offersDir = new File(OFFERS_PATH);

        if (!offersDir.exists()) {
            offersDir.mkdirs();
        }
    }

    Observable<List<Offer>> getAll() {

        return Observable.fromArray(offersDir.list())
                .filter(fileName -> fileName != null && fileName.endsWith(".json"))
                .map(fileName -> fileName.substring(0, fileName.lastIndexOf('.')))
                .flatMapMaybe(this::read)
                .toList().toObservable();
    }

    Single<Offer> write(Offer offer) {

        return Single.<Offer>create(source -> {
            try {
                String offerFileName = String.format("%s%s.%s", OFFERS_PATH, offer.getId(), JSON_EXT);
                File offerFile = new File(offerFileName);
                String offerJson = gson.toJson(offer);
                Files.write(offerJson.getBytes(Charset.defaultCharset()), offerFile);
                source.onSuccess(offer);
            } catch (Exception e) {
                source.onError(new OfferException(String.format("Could not write offer: %s", offer)));
            }
        }).retryWhen(new RetryWithDelay(3, 500, TimeUnit.MILLISECONDS))
                .doOnError(t -> log.error("write error: {}", t.getMessage()));
    }

    Maybe<Offer> read(String id) {

        return Maybe.<Offer>create(source -> {
            try {
                String offerFileName = String.format("%s%s.%s", OFFERS_PATH, id, JSON_EXT);
                File offerFile = new File(offerFileName);
                String offerJson = Files.toString(offerFile, Charset.defaultCharset());
                Offer offer = gson.fromJson(offerJson, Offer.class);
                source.onSuccess(offer);
            } catch (Exception e) {
                source.onError(new OfferException(String.format("Could not read offer id: %s", id)));
            }
        })
                .retryWhen(new RetryWithDelay(3, 500, TimeUnit.MILLISECONDS))
                .doOnError(t -> log.error("read error: {}", t.getMessage()));
    }

    Single<String> delete(String id) {
        return Single.create(source -> {
            try {
                String offerFileName = String.format("%s%s.%s", OFFERS_PATH, id, JSON_EXT);
                File offerFile = new File(offerFileName);
                offerFile.delete();
                source.onSuccess(id);
            } catch (Exception ex) {
                log.error("Could not delete offer id: {}", id);
                source.onSuccess(id);
            }
        });
    }
}
