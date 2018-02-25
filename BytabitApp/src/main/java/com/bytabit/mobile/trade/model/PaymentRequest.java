package com.bytabit.mobile.trade.model;

public class PaymentRequest {

    private String fundingTxHash;
    private String paymentDetails;
    private String refundAddress;
    private String refundTxSignature;

    public PaymentRequest(String fundingTxHash, String paymentDetails, String refundAddress, String refundTxSignature) {
        this.fundingTxHash = fundingTxHash;
        this.paymentDetails = paymentDetails;
        this.refundAddress = refundAddress;
        this.refundTxSignature = refundTxSignature;
    }

    public String getFundingTxHash() {
        return fundingTxHash;
    }

    public String getPaymentDetails() {
        return paymentDetails;
    }

    public String getRefundAddress() {
        return refundAddress;
    }

    public String getRefundTxSignature() {
        return refundTxSignature;
    }
}
