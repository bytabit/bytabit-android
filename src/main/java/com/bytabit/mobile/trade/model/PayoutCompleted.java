package com.bytabit.mobile.trade.model;

public class PayoutCompleted {

    public enum Reason {
        SELLER_BUYER_PAYOUT, ARBITRATOR_SELLER_REFUND, ARBITRATOR_BUYER_PAYOUT, BUYER_SELLER_REFUND
    }

    private String payoutTxHash;
    private Reason reason;

    public PayoutCompleted(String payoutTxHash, Reason reason) {
        this.payoutTxHash = payoutTxHash;
        this.reason = reason;
    }

    public String getPayoutTxHash() {
        return payoutTxHash;
    }

    public Reason getReason() {
        return reason;
    }
}
