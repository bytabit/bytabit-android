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

import com.auth0.android.jwt.JWT;
import com.bytabit.app.core.common.RetryWithDelay;
import com.bytabit.app.core.net.ServiceApiFactory;
import com.bytabit.app.core.net.TorManager;
import com.bytabit.app.core.wallet.model.DojoAuthResponse;
import com.bytabit.app.core.wallet.model.DojoHdAccountResponse;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DojoService {

    private final ServiceApiFactory serviceApiFactory;
    private final TorManager torManager;

    private BehaviorSubject<Single<DojoAuthResponse.Authorizations>> authorizations;

    // TODO these should be read from pairing QR Code and saved to and reloaded from local storage
    private final String apiKey = "myApiKey";
    private final String baseUrl = "http://z6xllfv63ftjw6cgnhmot5qt67n6ft7fre774jul64ix6dcu3bms5cid.onion/test/v2/";

    @Inject
    public DojoService(ServiceApiFactory serviceApiFactory, TorManager torManager) {
        this.serviceApiFactory = serviceApiFactory;
        this.torManager = torManager;

        this.authorizations = BehaviorSubject.createDefault(login());
    }

    //curl -x socks5h://localhost:9050 -X POST http://z6xllfv63ftjw6cgnhmot5qt67n6ft7fre774jul64ix6dcu3bms5cid.onion/test/v2/auth/login?apikey=myApiKey

    private Single<DojoAuthResponse.Authorizations> login() {

        return torManager.getTorState()
                .filter(s -> s.equals(TorManager.State.CONNECTED))
                .firstOrError()
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .flatMap(s -> serviceApiFactory.createService(baseUrl, DojoServiceApi.class, null)
                        .login(apiKey)
                        .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS)))
                .map(DojoAuthResponse::getAuthorizations)
                .doOnSuccess(a -> log.debug("successful login: accessToken: {}", a.getAccessToken()))
                .doOnSuccess(this::setAuthorizations)
                .doOnError(t -> log.error("login error: {}", t.getMessage()));
    }

    public Single<DojoAuthResponse.Authorizations> loginIfNeeded() {
        return authorizations
                .firstOrError()
                .flatMap(a -> a)
                .filter(a -> !isAccessTokenExpired(a))
                .doOnSuccess(a -> log.debug("saved valid authorization found. accessToken: {}", a.getAccessToken()))
                .switchIfEmpty(login());
    }

    private void setAuthorizations(DojoAuthResponse.Authorizations authorizations) {
        this.authorizations.onNext(Single.just(authorizations));
        log.debug("authorizations saved. accessToken: {}", authorizations.getAccessToken());
    }

    private boolean isAccessTokenExpired(DojoAuthResponse.Authorizations authorizations) {
        JWT jwt = new JWT(authorizations.getAccessToken());
        return jwt.isExpired(0);
    }

    public Single<DojoHdAccountResponse.HdAccount> getHdAccount(String xpub) {
        return loginIfNeeded().flatMap(a -> serviceApiFactory.createService(baseUrl, DojoServiceApi.class, a.getAccessToken())
                .getHdAccount(xpub).map(DojoHdAccountResponse::getData));
    }

}
