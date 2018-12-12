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
                .escrowAddress(tr.getTrade().getEscrowAddress())
                .offer(tr.getTrade().getSellOffer())
                .takeOfferRequest(tr.getTrade().getBuyRequest())
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
