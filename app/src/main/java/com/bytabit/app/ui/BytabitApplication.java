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

package com.bytabit.app.ui;

import android.app.Application;

import com.bytabit.app.ApplicationComponent;
import com.bytabit.app.BuildConfig;
import com.bytabit.app.DaggerApplicationComponent;
import com.bytabit.app.core.common.AppConfig;

import java.util.concurrent.Executors;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class BytabitApplication extends Application {

    private ApplicationComponent applicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        AppConfig appConfig = AppConfig.builder()
                .appStorage(getFilesDir())
                .version(BuildConfig.VERSION_NAME)
                .btcNetwork(BuildConfig.BTC_NETWORK)
                .configName(BuildConfig.CONFIG_NAME)
                .baseUrl(BuildConfig.BASE_URL)
                .peerAddress(BuildConfig.PEER_ADDRESS)
                .peerPort(BuildConfig.PEER_PORT)
                .build();

        applicationComponent = DaggerApplicationComponent.builder()
                .appConfig(appConfig)
                .walletExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()))
                .build();
    }

    public Single<ApplicationComponent> getApplicationComponent() {
        return Single.just(applicationComponent)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }
}
