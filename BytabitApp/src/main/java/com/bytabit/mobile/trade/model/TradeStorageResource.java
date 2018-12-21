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
