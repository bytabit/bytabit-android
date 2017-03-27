package com.bytabit.mobile.trade.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PayoutRequest {

    public PayoutRequest() {
    }

    private final StringProperty buyerEscrowPubKey = new SimpleStringProperty();
    private final StringProperty paymentId = new SimpleStringProperty();
    private final StringProperty payoutSignature = new SimpleStringProperty();

    public String getBuyerEscrowPubKey() {
        return buyerEscrowPubKey.get();
    }

    public StringProperty buyerEscrowPubKeyProperty() {
        return buyerEscrowPubKey;
    }

    public void setBuyerEscrowPubKey(String buyerEscrowPubKey) {
        this.buyerEscrowPubKey.set(buyerEscrowPubKey);
    }

    public String getPaymentId() {
        return paymentId.get();
    }

    public StringProperty paymentIdProperty() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId.set(paymentId);
    }

    public String getPayoutSignature() {
        return payoutSignature.get();
    }

    public StringProperty payoutSignatureProperty() {
        return payoutSignature;
    }

    public void setPayoutSignature(String payoutSignature) {
        this.payoutSignature.set(payoutSignature);
    }
}
