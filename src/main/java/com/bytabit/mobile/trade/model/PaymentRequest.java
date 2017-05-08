package com.bytabit.mobile.trade.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PaymentRequest {

    public PaymentRequest() {
    }

    private final StringProperty escrowAddress = new SimpleStringProperty();
    private final StringProperty fundingTxHash = new SimpleStringProperty();
    private final StringProperty paymentDetails = new SimpleStringProperty();

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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PaymentRequest{");
        sb.append("escrowAddress=").append(escrowAddress);
        sb.append(", fundingTxHash=").append(fundingTxHash);
        sb.append(", paymentDetails=").append(paymentDetails);
        sb.append('}');
        return sb.toString();
    }
}
