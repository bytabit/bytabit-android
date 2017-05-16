package com.bytabit.mobile.trade.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PayoutRequest {

    public PayoutRequest() {
    }

    private final StringProperty escrowAddress = new SimpleStringProperty();
    private final StringProperty paymentId = new SimpleStringProperty();
    private final StringProperty payoutSignature = new SimpleStringProperty();

    public String getEscrowAddress() {
        return escrowAddress.get();
    }

    public StringProperty escrowAddressProperty() {
        return escrowAddress;
    }

    public void setEscrowAddress(String escrowAddress) {
        this.escrowAddress.set(escrowAddress);
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
