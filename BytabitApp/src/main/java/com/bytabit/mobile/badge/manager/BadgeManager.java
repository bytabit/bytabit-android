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
import com.bytabit.mobile.badge.model.BadgeRequest;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class BadgeManager {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PublishSubject<Badge> createdBadge;

    private final BadgeService badgeService;

    private final BadgeStorage badgeStorage;

//    private Observable<Badge> badges;

//    private final Gson gson;

    @Inject
    ArbitratorManager arbitratorManager;

    @Inject
    WalletManager walletManager;

    public BadgeManager() {

//        gson = new GsonBuilder()
//                .setPrettyPrinting()
//                .registerTypeAdapter(Date.class, new DateConverter())
//                .create();

        badgeService = new BadgeService();

        badgeStorage = new BadgeStorage();

        createdBadge = PublishSubject.create();
    }

    @PostConstruct
    public void initialize() {

        //badges = badgeStorage.getAll().flattenAsObservable(bl -> bl).concatWith(createdBadge).share();
    }

    public Maybe<Badge> getOfferMakerBadge(CurrencyCode currencyCode) {

        return badgeStorage.getAll().flattenAsObservable(b -> b)
                .filter(b -> b.getBadgeType().equals(Badge.BadgeType.OFFER_MAKER))
                .filter(b -> b.getCurrencyCode().equals(currencyCode))
                .filter(b -> b.getValidFrom().compareTo(new Date()) <= 0)
                .filter(b -> b.getValidTo().compareTo(new Date()) >= 0)
                .firstElement();
    }

    public Single<List<Badge>> getLoadedBadges() {
        return badgeStorage.getAll();
    }

    public Observable<Badge> getCreatedBadges() {
        return createdBadge.share();
    }

    public Maybe<Badge> buyBadge(Badge.BadgeType badgeType, CurrencyCode currencyCode, BigDecimal priceBtcAmount,
                                 Date validFrom, Date validTo) {

        if (badgeType == null) {
            throw new BadgeException("Badge type required to create badge.");
        }

        if (currencyCode == null) {
            throw new BadgeException("Currency code required to create badge.");
        }

        if (priceBtcAmount == null) {
            throw new BadgeException("Price BTC amount required to create badge.");
        }

        if (validFrom == null) {
            throw new BadgeException("Valid from date required to create badge.");
        }

        if (validTo == null) {
            throw new BadgeException("Valid to date required to create badge.");
        }

        Maybe<TransactionWithAmt> paymentTransaction = walletManager.withdrawFromTradeWallet(arbitratorManager.getArbitrator().getFeeAddress(), priceBtcAmount);
        Maybe<String> profilePubKeyBase58 = walletManager.getProfilePubKeyBase58();

        return Maybe.zip(paymentTransaction, profilePubKeyBase58, (tx, pubKey) -> {

            Badge badge = Badge.builder()
                    .profilePubKey(pubKey)
                    .badgeType(Badge.BadgeType.OFFER_MAKER)
                    .currencyCode(currencyCode)
                    .validFrom(validFrom)
                    .validTo(validTo)
                    .build();

            return BadgeRequest.builder()
                    .badge(badge)
                    .btcAmount(priceBtcAmount)
                    .transactionHash(tx.getHash())
                    .build();
        })
                .observeOn(Schedulers.io())
                .flatMapSingleElement(badgeService::put)
                .flatMapSingleElement(badgeStorage::write)
                .doOnSuccess(createdBadge::onNext);
    }
}