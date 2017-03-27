package com.bytabit.mobile.trade.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PaymentRequest {

    public PaymentRequest() {
    }

    private final StringProperty buyerEscrowPubKey = new SimpleStringProperty();
    private final StringProperty fundingTxId = new SimpleStringProperty();
    private final StringProperty paymentDetails = new SimpleStringProperty();

    public String getBuyerEscrowPubKey() {
        return buyerEscrowPubKey.get();
    }

    public StringProperty buyerEscrowPubKeyProperty() {
        return buyerEscrowPubKey;
    }

    public void setBuyerEscrowPubKey(String buyerEscrowPubKey) {
        this.buyerEscrowPubKey.set(buyerEscrowPubKey);
    }

    public String getFundingTxId() {
        return fundingTxId.get();
    }

    public StringProperty fundingTxIdProperty() {
        return fundingTxId;
    }

    public void setFundingTxId(String fundingTxId) {
        this.fundingTxId.set(fundingTxId);
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
        sb.append("buyerEscrowPubKey=").append(buyerEscrowPubKey);
        sb.append(", fundingTxId=").append(fundingTxId);
        sb.append(", paymentDetails=").append(paymentDetails);
        sb.append('}');
        return sb.toString();
    }
}
