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
import com.bytabit.mobile.common.DateConverter;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

public class BadgeManager {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PublishSubject<Badge> createdBadge;

    private final BadgeService badgeService;

    private final BadgeStorage badgeStorage;

    private Observable<Badge> badges;

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

        badgeStorage = new BadgeStorage();

        createdBadge = PublishSubject.create();
    }

    @PostConstruct
    public void initialize() {

        badges = badgeStorage.getAll().flattenAsObservable(bl -> bl).concatWith(createdBadge).share();
    }

    public Maybe<Badge> getOfferMakerBadge(CurrencyCode currencyCode) {

        return badgeStorage.getAll().flattenAsObservable(b -> b)
                .filter(b -> b.getBadgeType().equals(Badge.BadgeType.OFFER_MAKER))
                .filter(b -> b.getCurrencyCode().equals(currencyCode))
                .filter(b -> b.getValidFrom().compareTo(new Date()) <= 0)
                .filter(b -> b.getValidTo().compareTo(new Date()) >= 0)
                .firstElement();
    }

    public Maybe<Badge> createOfferMakerBadge(CurrencyCode currencyCode) {

        if (currencyCode == null) {
            throw new BadgeException("Currency code required to create offer maker badge.");
        }

        BigDecimal badgePrice = BigDecimal.valueOf(0.0025); // TODO need calculate badge btc price

        Maybe<TransactionWithAmt> paymentTransaction = walletManager.withdrawFromTradeWallet(arbitratorManager.getArbitrator().getFeeAddress(), badgePrice);
        Maybe<String> profilePubKeyBase58 = walletManager.getProfilePubKeyBase58();

        return Maybe.zip(paymentTransaction, profilePubKeyBase58, (tx, pubKey) -> {

            Date validFrom = new Date();
            Calendar c = Calendar.getInstance();
            c.setTime(validFrom);
            c.add(Calendar.YEAR, 1);
            Date validTo = c.getTime();

            Badge badge = Badge.builder()
                    .profilePubKey(pubKey)
                    .badgeType(Badge.BadgeType.OFFER_MAKER)
                    .currencyCode(currencyCode)
                    .validFrom(validFrom)
                    .validTo(validTo)
                    .build();

            return BadgeRequest.builder()
                    .badge(badge)
                    .btcAmount(tx.getTransactionBigDecimalAmt())
                    .transactionHash(tx.getHash())
                    .build();
        })
                .observeOn(Schedulers.io())
                .flatMapSingleElement(badgeService::put)
                .flatMapSingleElement(badgeStorage::write)
                .doOnSuccess(createdBadge::onNext);
    }
}