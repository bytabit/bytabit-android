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

import java.time.ZonedDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class TradeStorageResource {

    private Long version;

    private Trade.Status status;

    private Trade.Role role;

    private String createdTimestamp;

    private TradeResource trade;

    public static Trade toTrade(TradeStorageResource tr) {
        return Trade.builder()
                .version(tr.getVersion())
                .status(tr.getStatus())
                .role(tr.getRole())
                .createdTimestamp(ZonedDateTime.parse(tr.getCreatedTimestamp()))
                .offer(tr.getTrade().getOffer())
                .tradeRequest(tr.getTrade().getTakeOfferRequest())
                .tradeAcceptance(tr.getTrade().getConfirmation())
                .paymentRequest(tr.getTrade().getPaymentRequest())
                .payoutRequest(tr.getTrade().getPayoutRequest())
                .arbitrateRequest(tr.getTrade().getArbitrateRequest())
                .cancelCompleted(tr.getTrade().getCancelCompleted())
                .payoutCompleted(tr.getTrade().getPayoutCompleted())
                .build();
    }

    public static TradeStorageResource fromTrade(Trade t) {
        return TradeStorageResource.builder()
                .version(t.getVersion())
                .status(t.getStatus())
                .role(t.getRole())
                .createdTimestamp(t.getCreatedTimestamp().toString())
                .trade(TradeResource.fromTrade(t))
                .build();
    }
}
