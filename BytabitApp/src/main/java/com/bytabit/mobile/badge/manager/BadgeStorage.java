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

package com.bytabit.mobile.badge.manager;

import com.bytabit.mobile.badge.model.Badge;
import com.bytabit.mobile.common.DateConverter;
import com.bytabit.mobile.common.RetryWithDelay;
import com.bytabit.mobile.config.AppConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Strings;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BadgeStorage {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String BADGE_PATH = AppConfig.getPrivateStorage().getPath() + File.separator +
            "badges" + File.separator;

    private static final String JSON_EXT = "json";

    private final Gson gson;

    private final File badgesDir;

    BadgeStorage() {

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Date.class, new DateConverter())
                .create();

        badgesDir = new File(BADGE_PATH);

        if (!badgesDir.exists()) {
            badgesDir.mkdirs();
        }
    }

    Single<List<Badge>> getAll() {

        return Observable.fromArray(badgesDir.list())
                .filter(fileName -> fileName != null && fileName.endsWith(".json"))
                .map(fileName -> fileName.substring(0, fileName.lastIndexOf('.')))
                .flatMapMaybe(this::read)
                .toList();
    }

    Single<Badge> write(Badge badge) {

        return Single.<Badge>create(source -> {
            try {
                String badgeFileName = String.format("%s%s.%s", BADGE_PATH, badge.getId(), JSON_EXT);
                File badgeFile = new File(badgeFileName);
                String badgeJson = gson.toJson(badge);
                Files.write(badgeFile.toPath(), Strings.toByteArray(badgeJson));
                source.onSuccess(badge);
            } catch (Exception e) {
                source.onError(new BadgeException(String.format("Could not write badge: %s", badge)));
            }
        }).retryWhen(new RetryWithDelay(3, 500, TimeUnit.MILLISECONDS))
                .doOnError(t -> log.error("write error: {}", t.getMessage()));
    }

    Maybe<Badge> read(String id) {

        return Maybe.<Badge>create(source -> {
            try {
                String badgeFileName = String.format("%s%s.%s", BADGE_PATH, id, JSON_EXT);
                File badgeFile = new File(badgeFileName);
                byte[] encoded = Files.readAllBytes(badgeFile.toPath());
                String badgeJson = new String(encoded, Charset.defaultCharset());
                Badge badge = gson.fromJson(badgeJson, Badge.class);
                source.onSuccess(badge);
            } catch (Exception e) {
                source.onError(new BadgeException(String.format("Could not read badge id: %s", id)));
            }
        })
                .retryWhen(new RetryWithDelay(3, 500, TimeUnit.MILLISECONDS))
                .doOnError(t -> log.error("read error: {}", t.getMessage()));
    }

    Single<String> delete(String id) {
        return Single.create(source -> {
            try {
                String badgeFileName = String.format("%s%s.%s", BADGE_PATH, id, JSON_EXT);
                File badgeFile = new File(badgeFileName);
                Files.delete(badgeFile.toPath());
                source.onSuccess(id);
            } catch (Exception ex) {
                log.error("Could not delete badge id: {}", id);
                source.onSuccess(id);
            }
        });
    }
}
