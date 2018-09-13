package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.SellOffer;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class TradeResource {

    private String escrowAddress;

    // Sell Offer
    private SellOffer sellOffer;

    // Buy Request
    private BuyRequest buyRequest;

    // Funding, Payment Request
    private PaymentRequest paymentRequest;

    // Payout Request
    private PayoutRequest payoutRequest;

    // Arbitrate Request
    private ArbitrateRequest arbitrateRequest;

    // Payout Completed
    private PayoutCompleted payoutCompleted;

    public static Trade toTrade(TradeResource tr) {
        return Trade.builder()
                .escrowAddress(tr.getEscrowAddress())
                .sellOffer(tr.getSellOffer())
                .buyRequest(tr.getBuyRequest())
                .paymentRequest(tr.getPaymentRequest())
                .payoutRequest(tr.getPayoutRequest())
                .arbitrateRequest(tr.getArbitrateRequest())
                .payoutCompleted(tr.getPayoutCompleted())
                .build();
    }

    public static TradeResource fromTrade(Trade t) {
        return TradeResource.builder()
                .escrowAddress(t.getEscrowAddress())
                .sellOffer(t.getSellOffer())
                .buyRequest(t.getBuyRequest())
                .paymentRequest(t.getPaymentRequest())
                .payoutRequest(t.getPayoutRequest())
                .arbitrateRequest(t.getArbitrateRequest())
                .payoutCompleted(t.getPayoutCompleted())
                .build();
    }

}
