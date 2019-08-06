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

import com.bytabit.app.core.common.file.Entity;
import com.bytabit.app.core.offer.model.Offer;

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
public class TradeResource implements Entity {

    private String id;

    @NonNull
    private Offer offer;

    @NonNull
    private TradeRequest tradeRequest;

    private TradeAcceptance tradeAcceptance;

    private PaymentRequest paymentRequest;

    private PayoutRequest payoutRequest;

    private ArbitrateRequest arbitrateRequest;

    private CancelCompleted cancelCompleted;

    private PayoutCompleted payoutCompleted;

    public static Trade toTrade(TradeResource tr) {
        return Trade.builder()
                .id(tr.getId())
                .offer(tr.getOffer())
                .tradeRequest(tr.getTradeRequest())
                .tradeAcceptance(tr.getTradeAcceptance())
                .paymentRequest(tr.getPaymentRequest())
                .payoutRequest(tr.getPayoutRequest())
                .arbitrateRequest(tr.getArbitrateRequest())
                .cancelCompleted(tr.getCancelCompleted())
                .payoutCompleted(tr.getPayoutCompleted())
                .build();
    }

    public static TradeResource fromTrade(Trade t) {
        return TradeResource.builder()
                .id(t.getId())
                .offer(t.getOffer())
                .tradeRequest(t.getTradeRequest())
                .tradeAcceptance(t.getTradeAcceptance())
                .paymentRequest(t.getPaymentRequest())
                .payoutRequest(t.getPayoutRequest())
                .arbitrateRequest(t.getArbitrateRequest())
                .cancelCompleted(t.getCancelCompleted())
                .payoutCompleted(t.getPayoutCompleted())
                .build();
    }

}
