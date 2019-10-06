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


import com.bytabit.app.core.arbitrate.ArbitratorManager;
import com.bytabit.app.core.badge.model.Badge;
import com.bytabit.app.core.badge.model.BadgeRequest;
import com.bytabit.app.core.payment.model.CurrencyCode;
import com.bytabit.app.core.wallet.WalletManager;
import com.bytabit.app.core.wallet.model.TransactionWithAmt;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BadgeManager {

    private final BadgeService badgeService;

    private final BadgeStorage badgeStorage;

    private final ArbitratorManager arbitratorManager;

    private final WalletManager walletManager;

    @Inject
    public BadgeManager(ArbitratorManager arbitratorManager, WalletManager walletManager,
                        BadgeService badgeService, BadgeStorage badgeStorage) {

        this.arbitratorManager = arbitratorManager;
        this.walletManager = walletManager;
        this.badgeService = badgeService;
        this.badgeStorage = badgeStorage;
    }

    public Single<Badge> getOfferMakerBadge(CurrencyCode currencyCode) {

        return badgeStorage.getAll().flattenAsObservable(b -> b)
                .filter(b -> b.getBadgeType().equals(Badge.BadgeType.OFFER_MAKER))
                .filter(b -> b.getCurrencyCode().equals(currencyCode))
                .filter(b -> b.getValidFrom().compareTo(new Date()) <= 0)
                .filter(b -> b.getValidTo().compareTo(new Date()) >= 0)
                .firstElement().toSingle()
                .onErrorResumeNext(t -> {
                    if (t instanceof NoSuchElementException) {
                        return Single.error(new BadgeException(String.format("Please buy an offer maker badge for %s.", currencyCode)));
                    } else {
                        return Single.error(t);
                    }
                });
    }

    public Single<List<Badge>> getStoredBadges() {
        return badgeStorage.getAll();
    }

    public Maybe<Badge> buyBadge(Badge.BadgeType badgeType, CurrencyCode currencyCode, BigDecimal priceBtcAmount,
                                 Date validFrom, Date validTo) {

        if (badgeType == null) {
            return Maybe.error(new BadgeException("Badge type required to create badge."));
        }

        if (currencyCode == null) {
            return Maybe.error(new BadgeException("Currency code required to create badge."));
        }

        if (priceBtcAmount == null) {
            return Maybe.error(new BadgeException("Price BTC amount required to create badge."));
        }

        if (validFrom == null) {
            return Maybe.error(new BadgeException("Valid from date required to create badge."));
        }

        if (validTo == null) {
            return Maybe.error(new BadgeException("Valid to date required to create badge."));
        }

        Maybe<TransactionWithAmt> paymentTransaction = walletManager.withdrawFromTradeWallet(arbitratorManager.getArbitrator().getFeeAddress(), priceBtcAmount, walletManager.defaultTxFee());
        Maybe<String> profilePubKeyBase58 = walletManager.getProfilePubKeyBase58();

        return Maybe.zip(paymentTransaction, profilePubKeyBase58, (tx, pubKey) -> {

            Badge badge = Badge.builder()
                    .id(UUID.randomUUID().toString())
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
                .flatMapSingleElement(badgeService::put)
                .flatMapSingleElement(badgeStorage::write)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io());
    }
}