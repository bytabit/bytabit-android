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

package com.bytabit.app.core.net;

import com.bytabit.app.core.common.json.DateConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.Proxy;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Slf4j
@Singleton
public class ServiceApiFactory {

    private final TorManager torManager;
    private final Gson gson;

    private String accessToken;
    private String refreshToken;

    @Inject
    public ServiceApiFactory(TorManager torManager) {
        this.torManager = torManager;

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Date.class, new DateConverter())
                .create();
    }

    public <S> S createService(String baseUrl, Class<S> serviceClass) {
        return createService(baseUrl, serviceClass, null);
    }

    public <S> S createService(String baseUrl, Class<S> serviceClass, String authorizationToken) {

        Proxy proxy = torManager.getTorProxy();

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .proxy(proxy);

        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            clientBuilder.addInterceptor(chain -> {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .header("Authorization", authorizationToken)
                        .build();
                return chain.proceed(request);
            });
        }

        OkHttpClient client = clientBuilder.build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        return retrofit.create(serviceClass);
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
