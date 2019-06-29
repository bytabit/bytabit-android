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

import com.bytabit.app.core.payment.model.CurrencyCode;
import com.bytabit.app.core.payment.model.PaymentMethod;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class SignedOffer extends Offer {

    @NonNull
    private String signature;

    @Builder(builderMethodName = "signedBuilder")
    public SignedOffer(String id, @NonNull OfferType offerType, @NonNull String makerProfilePubKey,
                       @NonNull CurrencyCode currencyCode, @NonNull PaymentMethod paymentMethod,
                       @NonNull BigDecimal minAmount, @NonNull BigDecimal maxAmount,
                       @NonNull BigDecimal price, Boolean isMine, @NonNull String signature) {

        super(id, offerType, makerProfilePubKey, currencyCode, paymentMethod, minAmount, maxAmount, price, isMine);
        this.signature = signature;
    }
}
