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
                .escrowAddress(receivedTradeServiceResource.getEscrowAddress())
                .version(receivedTradeServiceResource.getVersion())
                .sellOffer(receivedTradeServiceResource.getTrade().getSellOffer())
                .buyRequest(receivedTradeServiceResource.getTrade().getBuyRequest())
                .paymentRequest(receivedTradeServiceResource.getTrade().getPaymentRequest())
                .payoutRequest(receivedTradeServiceResource.getTrade().getPayoutRequest())
                .arbitrateRequest(receivedTradeServiceResource.getTrade().getArbitrateRequest())
                .cancelCompleted(receivedTradeServiceResource.getTrade().getCancelCompleted())
                .payoutCompleted(receivedTradeServiceResource.getTrade().getPayoutCompleted());
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
