package com.bytabit.mobile.trade.model;

public class PayoutRequest {

    private String paymentReference;
    private String payoutTxSignature;

    public PayoutRequest(String paymentReference, String payoutTxSignature) {
        this.paymentReference = paymentReference;
        this.payoutTxSignature = payoutTxSignature;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getPayoutTxSignature() {
        return payoutTxSignature;
    }
}
