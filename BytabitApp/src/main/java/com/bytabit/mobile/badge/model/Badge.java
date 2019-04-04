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

package com.bytabit.mobile.badge.model;

import com.bytabit.mobile.common.Entity;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import lombok.*;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Badge implements Entity {

    public enum BadgeType {
        BETA_TESTER,
        OFFER_MAKER,
        DETAILS_VERIFIED
    }

    @Builder
    public Badge(@NonNull String profilePubKey, @NonNull BadgeType badgeType,
                 @NonNull Date validFrom, @NonNull Date validTo,
                 CurrencyCode currencyCode, PaymentMethod paymentMethod, String detailsHash) {

        this.profilePubKey = profilePubKey;
        this.badgeType = badgeType;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.currencyCode = currencyCode;
        this.paymentMethod = paymentMethod;
        this.detailsHash = detailsHash;
        this.id = getId();
    }

    @Getter(AccessLevel.NONE)
    private String id;

    private String profilePubKey;

    private BadgeType badgeType;

    private Date validFrom;

    private Date validTo;

    private CurrencyCode currencyCode;

    private PaymentMethod paymentMethod;

    private String detailsHash;

    // Use Hex encoded Sha256 Hash of badge parameters
    public String getId() {
        if (id == null) {
            String idString = String.format("|%s|%s|%s|%s|%s|%s|%s|",
                    profilePubKey, badgeType,
                    validFrom, validTo,
                    currencyCode, paymentMethod, detailsHash);

            id = Base58.encode(Sha256Hash.of(idString.getBytes()).getBytes());
        }
        return id;
    }
}
