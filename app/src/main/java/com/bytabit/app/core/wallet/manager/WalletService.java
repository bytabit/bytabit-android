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

package com.bytabit.app.core.wallet.manager;

import com.bytabit.app.core.common.RetryWithDelay;
import com.bytabit.app.core.common.net.ServiceApiFactory;
import com.bytabit.app.core.wallet.model.LoginTokens;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class WalletService {

    private final ServiceApiFactory serviceApiFactory;

    @Inject
    public WalletService(ServiceApiFactory serviceApiFactory) {
        this.serviceApiFactory = serviceApiFactory;
    }

    //curl -x socks5h://localhost:9050 -X POST http://z6xllfv63ftjw6cgnhmot5qt67n6ft7fre774jul64ix6dcu3bms5cid.onion/test/v2/auth/login?apikey=myApiKey

    public Single<LoginTokens> login(String apiKey) {

        String baseUrl = "http://z6xllfv63ftjw6cgnhmot5qt67n6ft7fre774jul64ix6dcu3bms5cid.onion/test/v2/";

        return serviceApiFactory.createService(baseUrl, WalletServiceApi.class, null)
                .login("myApiKey")
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("login error: {}", t.getMessage()));
        //.doOnSuccess(serviceApiFactory::setAccessToken);
        //.doOnSuccess(tokens -> log.debug("tokens: {}", tokens.toString()));
    }

}
