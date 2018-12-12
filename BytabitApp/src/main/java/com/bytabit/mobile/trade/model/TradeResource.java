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

    private String escrowAddress;

    // Sell Offer
    private Offer sellOffer;

    // Buy Request
    private TakeOfferRequest buyRequest;

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
                .escrowAddress(tr.getEscrowAddress())
                .offer(tr.getSellOffer())
                .takeOfferRequest(tr.getBuyRequest())
                .paymentRequest(tr.getPaymentRequest())
                .payoutRequest(tr.getPayoutRequest())
                .arbitrateRequest(tr.getArbitrateRequest())
                .cancelCompleted(tr.getCancelCompleted())
                .payoutCompleted(tr.getPayoutCompleted())
                .build();
    }

    public static TradeResource fromTrade(Trade t) {
        return TradeResource.builder()
                .escrowAddress(t.getEscrowAddress())
                .sellOffer(t.getOffer())
                .buyRequest(t.getTakeOfferRequest())
                .paymentRequest(t.getPaymentRequest())
                .payoutRequest(t.getPayoutRequest())
                .arbitrateRequest(t.getArbitrateRequest())
                .cancelCompleted(t.getCancelCompleted())
                .payoutCompleted(t.getPayoutCompleted())
                .build();
    }

}
