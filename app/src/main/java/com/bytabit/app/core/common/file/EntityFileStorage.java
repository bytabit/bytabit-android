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

package com.bytabit.app.core.common.file;

import com.bytabit.app.core.common.AppConfig;
import com.bytabit.app.core.common.RetryWithDelay;
import com.bytabit.app.core.common.json.DateConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class EntityFileStorage<T extends Entity> {

    private static final String JSON_EXT = ".json";

    private final Class<T> entityClass;

    private final String path;

    private final Gson gson;

    private final File filesDir;

    public EntityFileStorage(AppConfig appConfig, Class<T> entityClass) {

        this.entityClass = entityClass;

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Date.class, new DateConverter())
                .create();

        path = String.format("%s%s%s%s", appConfig.getAppStorage().getPath(), File.separator,
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
            try (FileOutputStream fos = new FileOutputStream(file)) {
                String entityJson = gson.toJson(entity);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.write(entityJson);
                osw.close();
                fos.flush();
                fos.close();
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
            try (FileInputStream fis = new FileInputStream(file)) {
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader bufferedReader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                String fileJson = sb.toString();
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
