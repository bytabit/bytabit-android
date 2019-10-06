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

package com.bytabit.app.core.badge;

import com.bytabit.app.core.badge.model.Badge;
import com.bytabit.app.core.badge.model.BadgeRequest;
import com.bytabit.app.core.common.AppConfig;
import com.bytabit.app.core.net.RetrofitService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BadgeService extends RetrofitService {

    private final BadgeServiceApi badgeServiceApi;

    @Inject
    public BadgeService(AppConfig appConfig) {
        super(appConfig);

        // create an instance of the ApiService
        badgeServiceApi = retrofit.create(BadgeServiceApi.class);
    }

    Single<Badge> put(BadgeRequest badgeRequest) {
        return badgeServiceApi.put(badgeRequest.getBadge().getProfilePubKey(), badgeRequest.getBadge().getId(), badgeRequest)
                .doOnError(t -> log.error("put error: {}", t.getMessage()));
    }

    Single<List<Badge>> getAll(String profilePubKey) {
        return badgeServiceApi.get(profilePubKey)
                .doOnError(t -> log.error("get error: {}", t.getMessage()));
    }
}
