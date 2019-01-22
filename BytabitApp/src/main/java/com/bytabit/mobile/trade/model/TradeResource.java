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

package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.Offer;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class TradeResource {

    // Offer
    private Offer offer;

    // Take Offer Request
    private TradeRequest takeOfferRequest;

    // Confirmation
    private TradeAcceptance confirmation;

    // Funding, Payment Request
    private PaymentRequest paymentRequest;

    // Payout Request
    private PayoutRequest payoutRequest;

    // Arbitrate Request
    private ArbitrateRequest arbitrateRequest;

    // Cancel Completed
    private CancelCompleted cancelCompleted;

    // Payout Completed
    private PayoutCompleted payoutCompleted;

    public static Trade toTrade(TradeResource tr) {
        return Trade.builder()
                .offer(tr.getOffer())
                .tradeRequest(tr.getTakeOfferRequest())
                .tradeAcceptance(tr.getConfirmation())
                .paymentRequest(tr.getPaymentRequest())
                .payoutRequest(tr.getPayoutRequest())
                .arbitrateRequest(tr.getArbitrateRequest())
                .cancelCompleted(tr.getCancelCompleted())
                .payoutCompleted(tr.getPayoutCompleted())
                .build();
    }

    public static TradeResource fromTrade(Trade t) {
        return TradeResource.builder()
                .offer(t.getOffer())
                .takeOfferRequest(t.getTradeRequest())
                .confirmation(t.getTradeAcceptance())
                .paymentRequest(t.getPaymentRequest())
                .payoutRequest(t.getPayoutRequest())
                .arbitrateRequest(t.getArbitrateRequest())
                .cancelCompleted(t.getCancelCompleted())
                .payoutCompleted(t.getPayoutCompleted())
                .build();
    }

}
