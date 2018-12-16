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
                .takeOfferRequest(receivedTradeServiceResource.getTrade().getTakeOfferRequest())
                .confirmation(receivedTradeServiceResource.getTrade().getConfirmation())
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
                .buyerProfilePubKey(t.getTakeOfferRequest().getTakerProfilePubKey())
                .trade(TradeResource.fromTrade(t));

        if (t.hasConfirmation()) {
            builder.arbitratorProfilePubKey(t.getConfirmation().getArbitratorProfilePubKey());
        }

        return builder.build();
    }
}
