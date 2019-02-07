package com.bytabit.mobile.badge.manager;

import com.bytabit.mobile.badge.model.Badge;
import com.bytabit.mobile.badge.model.BadgeRequest;
import com.bytabit.mobile.common.RetrofitService;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class BadgeService extends RetrofitService {

    private final BadgeServiceApi badgeServiceApi;

    public BadgeService() {

        // create an instance of the ApiService
        badgeServiceApi = retrofit.create(BadgeServiceApi.class);
    }

    Single<Badge> put(BadgeRequest badgeRequest) {
        return badgeServiceApi.put(badgeRequest.getBadge().getId(), badgeRequest)
                .doOnError(t -> log.error("put error: {}", t.getMessage()));
    }

    Single<List<Badge>> getAll(String profilePubKey) {
        return badgeServiceApi.get(profilePubKey)
                .doOnError(t -> log.error("get error: {}", t.getMessage()));
    }
}
