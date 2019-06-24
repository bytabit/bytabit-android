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

package com.bytabit.app.core.badge.model;

import com.bytabit.app.core.common.HashUtils;
import com.bytabit.app.core.common.file.Entity;
import com.bytabit.app.core.payment.model.CurrencyCode;
import com.bytabit.app.core.payment.model.PaymentMethod;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class Badge implements Entity {

    public enum BadgeType {
        BETA_TESTER(BigDecimal.valueOf(0.0001).setScale(8, RoundingMode.HALF_UP), 104),
        OFFER_MAKER(BigDecimal.valueOf(0.0025).setScale(8, RoundingMode.HALF_UP), 52),
        DETAILS_VERIFIED(BigDecimal.valueOf(.015).setScale(8, RoundingMode.HALF_UP), 26);

        BadgeType(BigDecimal price, int weeksValid) {
            this.price = price;
            this.weeksValid = weeksValid;
        }

        private final BigDecimal price;
        private final int weeksValid;

        public BigDecimal price() {
            return price;
        }

        public int weeksValid() {
            return weeksValid;
        }
    }

    @Getter(AccessLevel.NONE)
    private String id;

    @NonNull
    private String profilePubKey;

    @NonNull
    private BadgeType badgeType;

    @NonNull
    private Date validFrom;

    @NonNull
    private Date validTo;

    private CurrencyCode currencyCode;

    private PaymentMethod paymentMethod;

    private String detailsHash;

    public String getId() {
        if (id == null) {
            id = Base58.encode(sha256Hash().getBytes());
        }
        return id;
    }

    public Sha256Hash sha256Hash() {

        return HashUtils.sha256Hash(
                profilePubKey, badgeType, validFrom, validTo, currencyCode,
                paymentMethod, detailsHash);
    }
}
