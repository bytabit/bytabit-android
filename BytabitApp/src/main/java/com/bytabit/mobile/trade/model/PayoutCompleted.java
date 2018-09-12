package com.bytabit.mobile.trade.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PayoutCompleted {

    public enum Reason {
        SELLER_BUYER_PAYOUT, ARBITRATOR_SELLER_REFUND, ARBITRATOR_BUYER_PAYOUT, BUYER_SELLER_REFUND
    }

    private final String payoutTxHash;
    private final Reason reason;
}
