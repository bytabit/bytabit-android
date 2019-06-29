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

package com.bytabit.app.core.offer.model;

import com.bytabit.app.core.common.HashUtils;
import com.bytabit.app.core.common.file.Entity;
import com.bytabit.app.core.payment.model.CurrencyCode;
import com.bytabit.app.core.payment.model.PaymentMethod;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class Offer implements Entity {

    public enum OfferType {
        BUY,
        SELL
    }

    @Getter(AccessLevel.NONE)
    private String id;

    @NonNull
    private OfferType offerType;

    @NonNull
    private String makerProfilePubKey;

    @NonNull
    private CurrencyCode currencyCode;

    @NonNull
    private PaymentMethod paymentMethod;

    @NonNull
    private BigDecimal minAmount;

    @NonNull
    private BigDecimal maxAmount;

    @NonNull
    private BigDecimal price;

    private transient Boolean isMine;

    public String getId() {
        if (id == null) {
            id = Base58.encode(sha256Hash().getBytes());
        }
        return id;
    }

    public Sha256Hash sha256Hash() {

        CurrencyCode currencyCode = getCurrencyCode();

        return HashUtils.sha256Hash(getOfferType(), getMakerProfilePubKey(),
                getCurrencyCode(), getPaymentMethod(),
                getMinAmount().setScale(currencyCode.getScale(), RoundingMode.HALF_UP),
                getMaxAmount().setScale(currencyCode.getScale(), RoundingMode.HALF_UP),
                getPrice().setScale(currencyCode.getScale(), RoundingMode.HALF_UP));
    }
}
