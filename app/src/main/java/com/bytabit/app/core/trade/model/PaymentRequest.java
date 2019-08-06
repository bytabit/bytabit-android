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

import org.bitcoinj.core.Sha256Hash;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode
@ToString
public class PaymentRequest {

    @NonNull
    private String fundingTxHash;

    @NonNull
    private String paymentDetails;

    @NonNull
    private String refundAddress;

    @NonNull
    private String refundTxSignature;

    @NonNull
    private BigDecimal txFeePerKb;

    public Sha256Hash sha256Hash() {
        return HashUtils.sha256Hash(fundingTxHash, paymentDetails, refundAddress, refundTxSignature,
                txFeePerKb.setScale(8, RoundingMode.UP));
    }
}
