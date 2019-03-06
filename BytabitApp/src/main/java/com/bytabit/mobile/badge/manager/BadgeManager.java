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

import com.bytabit.mobile.arbitrate.manager.ArbitratorManager;
import com.bytabit.mobile.badge.model.Badge;
import com.bytabit.mobile.common.DateConverter;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BadgeManager {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PublishSubject<Badge> createdBadge;

    private final BadgeService badgeService;

    private Observable<List<Badge>> badges;

    private final Gson gson;

    @Inject
    ArbitratorManager arbitratorManager;

    @Inject
    WalletManager walletManager;

    public BadgeManager() {

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Date.class, new DateConverter())
                .create();

        badgeService = new BadgeService();

        createdBadge = PublishSubject.create();
    }

    @PostConstruct
    public void initialize() {

        badges = Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                .flatMap(tick -> getBadges());
    }

    public Observable<List<Badge>> getBadges() {
        return walletManager.getProfilePubKey().flatMap(pk -> badgeService.getAll(pk)
                .retryWhen(errors -> errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS)))
                .toObservable());
    }
}