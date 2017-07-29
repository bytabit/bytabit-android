package com.bytabit.mobile.trade.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PaymentRequest {

    public PaymentRequest() {
    }

    private final StringProperty escrowAddress = new SimpleStringProperty();
    private final StringProperty fundingTxHash = new SimpleStringProperty();
    private final StringProperty paymentDetails = new SimpleStringProperty();
    private final StringProperty refundAddress = new SimpleStringProperty();
    private final StringProperty refundTxSignature = new SimpleStringProperty();

    public String getEscrowAddress() {
        return escrowAddress.get();
    }

    public StringProperty escrowAddressProperty() {
        return escrowAddress;
    }

    public void setEscrowAddress(String escrowAddress) {
        this.escrowAddress.set(escrowAddress);
    }

    public String getFundingTxHash() {
        return fundingTxHash.get();
    }

    public StringProperty fundingTxHashProperty() {
        return fundingTxHash;
    }

    public void setFundingTxHash(String fundingTxHash) {
        this.fundingTxHash.set(fundingTxHash);
    }

    public String getPaymentDetails() {
        return paymentDetails.get();
    }

    public StringProperty paymentDetailsProperty() {
        return paymentDetails;
    }

    public void setPaymentDetails(String paymentDetails) {
        this.paymentDetails.set(paymentDetails);
    }

    public String getRefundAddress() {
        return refundAddress.get();
    }

    public StringProperty refundAddressProperty() {
        return refundAddress;
    }

    public void setRefundAddress(String refundAddress) {
        this.refundAddress.set(refundAddress);
    }

    public String getRefundTxSignature() {
        return refundTxSignature.get();
    }

    public StringProperty refundTxSignatureProperty() {
        return refundTxSignature;
    }

    public void setRefundTxSignature(String refundTxSignature) {
        this.refundTxSignature.set(refundTxSignature);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PaymentRequest{");
        sb.append("escrowAddress=").append(escrowAddress);
        sb.append(", fundingTxHash=").append(fundingTxHash);
        sb.append(", paymentDetails=").append(paymentDetails);
        sb.append(", refundAddress=").append(refundAddress);
        sb.append(", refundTxSignature=").append(refundTxSignature);
        sb.append('}');
        return sb.toString();
    }
}
