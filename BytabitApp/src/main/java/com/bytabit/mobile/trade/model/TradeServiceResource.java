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

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class TradeServiceResource {

    private String escrowAddress;

    private Long version;

    private String sellerProfilePubKey;

    private String buyerProfilePubKey;

    private String arbitratorProfilePubKey;

    private TradeResource trade;

    public static Trade toTrade(TradeServiceResource receivedTradeServiceResource) {

        return toTradeBuilder(receivedTradeServiceResource).build();
    }

    public static Trade toTrade(TradeServiceResource receivedTradeServiceResource, Trade trade) {

        return toTradeBuilder(receivedTradeServiceResource)
                .createdTimestamp(trade.getCreatedTimestamp())
                .role(trade.getRole())
                .status(trade.getStatus())
                .build();
    }

    private static Trade.TradeBuilder toTradeBuilder(TradeServiceResource receivedTradeServiceResource) {

        return Trade.builder()
                .version(receivedTradeServiceResource.getVersion())
                .offer(receivedTradeServiceResource.getTrade().getOffer())
                .tradeRequest(receivedTradeServiceResource.getTrade().getTakeOfferRequest())
                .tradeAcceptance(receivedTradeServiceResource.getTrade().getConfirmation())
                .paymentRequest(receivedTradeServiceResource.getTrade().getPaymentRequest())
                .payoutRequest(receivedTradeServiceResource.getTrade().getPayoutRequest())
                .arbitrateRequest(receivedTradeServiceResource.getTrade().getArbitrateRequest())
                .cancelCompleted(receivedTradeServiceResource.getTrade().getCancelCompleted())
                .payoutCompleted(receivedTradeServiceResource.getTrade().getPayoutCompleted());
    }

    public static TradeServiceResource fromTrade(Trade t) {

        TradeServiceResourceBuilder builder = TradeServiceResource.builder()
                .version(t.getVersion())
                .sellerProfilePubKey(t.getOffer().getMakerProfilePubKey())
                .buyerProfilePubKey(t.getTradeRequest().getTakerProfilePubKey())
                .trade(TradeResource.fromTrade(t));

        if (t.hasConfirmation()) {
            builder.arbitratorProfilePubKey(t.getTradeAcceptance().getArbitratorProfilePubKey());
        }

        return builder.build();
    }
}
