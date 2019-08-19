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

import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.wallet.model.TransactionWithAmt;

import java.util.Date;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class SignedTrade extends Trade {

    @NonNull
    private String signature;

    @Builder(builderMethodName = "signedBuilder")
    public SignedTrade(@NonNull String id, Long version, Status status, Role role,
                       @NonNull Date createdTimestamp, @NonNull Offer offer,
                       @NonNull TradeRequest tradeRequest, TradeAcceptance tradeAcceptance,
                       TransactionWithAmt fundingTransactionWithAmt,
                       PaymentRequest paymentRequest, PayoutRequest payoutRequest,
                       ArbitrateRequest arbitrateRequest,
                       TransactionWithAmt payoutTransactionWithAmt,
                       PayoutCompleted payoutCompleted,
                       CancelCompleted cancelCompleted, @NonNull String signature) {

        super(id, version, status, role, createdTimestamp, offer, tradeRequest, tradeAcceptance,
                fundingTransactionWithAmt, paymentRequest, payoutRequest, arbitrateRequest,
                payoutTransactionWithAmt, cancelCompleted, payoutCompleted);

        this.signature = signature;
    }
}
