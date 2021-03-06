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

package com.bytabit.app.core.trade.model;

import com.bytabit.app.core.common.HashUtils;
import com.bytabit.app.core.offer.model.Offer;

import org.bitcoinj.core.Sha256Hash;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class TradeRequest {

    @NonNull
    private String takerProfilePubKey;

    @NonNull
    private String takerEscrowPubKey;

    @NonNull
    private BigDecimal btcAmount;

    @NonNull
    private BigDecimal paymentAmount;

    public Sha256Hash sha256Hash(Offer offer) {
        return HashUtils.sha256Hash(takerProfilePubKey, takerEscrowPubKey,
                btcAmount.setScale(offer.getCurrencyCode().getScale(), RoundingMode.HALF_UP));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TradeRequest that = (TradeRequest) o;

        if (!takerProfilePubKey.equals(that.takerProfilePubKey)) return false;
        if (!takerEscrowPubKey.equals(that.takerEscrowPubKey)) return false;
        if (btcAmount.compareTo(that.btcAmount) != 0) return false;
        return paymentAmount.compareTo(that.paymentAmount) == 0;
    }

    @Override
    public int hashCode() {
        int result = takerProfilePubKey.hashCode();
        result = 31 * result + takerEscrowPubKey.hashCode();
        result = 31 * result + btcAmount.stripTrailingZeros().hashCode();
        result = 31 * result + paymentAmount.stripTrailingZeros().hashCode();
        return result;
    }
}
