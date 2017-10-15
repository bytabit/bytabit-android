package com.bytabit.mobile.trade.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class PayoutCompleted {

    public enum Reason {
        SELLER_BUYER_PAYOUT, ARBITRATOR_SELLER_REFUND, ARBITRATOR_BUYER_PAYOUT, BUYER_SELLER_REFUND
    }

    private String payoutTxHash;
    private Reason reason;
}
