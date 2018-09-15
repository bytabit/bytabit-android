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

    public static Trade toTrade(TradeServiceResource tr) {
        return Trade.builder()
                .escrowAddress(tr.getEscrowAddress())
                .version(tr.getVersion())
                .sellOffer(tr.getTrade().getSellOffer())
                .buyRequest(tr.getTrade().getBuyRequest())
                .paymentRequest(tr.getTrade().getPaymentRequest())
                .payoutRequest(tr.getTrade().getPayoutRequest())
                .arbitrateRequest(tr.getTrade().getArbitrateRequest())
                .payoutCompleted(tr.getTrade().getPayoutCompleted())
                .build();
    }

    public static TradeServiceResource fromTrade(Trade t) {
        return TradeServiceResource.builder()
                .escrowAddress(t.getEscrowAddress())
                .version(t.getVersion())
                .sellerProfilePubKey(t.getSellOffer().getSellerProfilePubKey())
                .buyerProfilePubKey(t.getBuyRequest().getBuyerProfilePubKey())
                .arbitratorProfilePubKey(t.getSellOffer().getArbitratorProfilePubKey())
                .trade(TradeResource.fromTrade(t))
                .build();
    }
}
