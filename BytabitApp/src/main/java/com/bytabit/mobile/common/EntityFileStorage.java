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

package com.bytabit.mobile.common;

import com.bytabit.mobile.config.AppConfig;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class EntityFileStorage<T extends Entity> {

    private static final String JSON_EXT = ".json";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final String path;

    private final Gson gson;

    private final File filesDir;

    private final Class<T> entityClass;

    public EntityFileStorage(Class<T> entityClass) {

        this.entityClass = entityClass;

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Date.class, new DateConverter())
                .create();

        path = String.format("%s%s%s%s", AppConfig.getPrivateStorage().getPath(), File.separator,
                entityClass.getSimpleName().toLowerCase(), File.separator);

        filesDir = new File(path);

        if (!filesDir.exists()) {
            filesDir.mkdirs();
        }
    }

    public Single<List<T>> getAll() {

        return Observable.fromArray(filesDir.list())
                .filter(fileName -> fileName != null && fileName.endsWith(JSON_EXT))
                .map(fileName -> fileName.substring(0, fileName.lastIndexOf('.')))
                .flatMapMaybe(this::read)
                .toList();
    }

    public Single<T> write(T entity) {

        return Single.<T>create(source -> {
            String fileName = fileName(entity.getId());
            File file = new File(fileName);
            try {
                String entityJson = gson.toJson(entity);
                Files.write(entityJson.getBytes(Charset.defaultCharset()), file);
                source.onSuccess(entity);
            } catch (Exception e) {
                source.onError(new FileStorageException(String.format("Could not write: %s", fileName)));
            }
        }).retryWhen(new RetryWithDelay(3, 500, TimeUnit.MILLISECONDS))
                .doOnError(t -> log.error("write error: {}", t.getMessage()));
    }

    public Maybe<T> read(String id) {

        return Maybe.<T>create(source -> {
            String fileName = fileName(id);
            File file = new File(fileName);
            try {
                String fileJson = Files.toString(file, Charset.defaultCharset());
                T entity = gson.fromJson(fileJson, entityClass);
                source.onSuccess(entity);
            } catch (FileNotFoundException fnfe) {
                log.warn("File not found: {}", fileName);
                source.onComplete();
            } catch (Exception e) {
                source.onError(new FileStorageException(String.format("Could not read: %s", fileName)));
            }
        })
                .retryWhen(new RetryWithDelay(3, 500, TimeUnit.MILLISECONDS))
                .doOnError(t -> log.error("read error: {}", t.getMessage()));
    }

    public Single<String> delete(String id) {
        return Single.create(source -> {
            String entityFileName = fileName(id);
            File entityFile = new File(entityFileName);
            try {
                entityFile.delete();
                source.onSuccess(id);
            } catch (Exception ex) {
                log.error("Could not delete: {}", entityFile);
                source.onSuccess(id);
            }
        });
    }

    private String fileName(String id) {
        return String.format("%s%s%s", path, id, JSON_EXT);
    }
}
