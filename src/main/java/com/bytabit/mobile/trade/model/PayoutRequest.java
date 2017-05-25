package com.bytabit.mobile.trade.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PayoutRequest {

    public PayoutRequest() {
    }

    private final StringProperty escrowAddress = new SimpleStringProperty();
    private final StringProperty paymentReference = new SimpleStringProperty();
    private final StringProperty payoutTxSignature = new SimpleStringProperty();

    public String getEscrowAddress() {
        return escrowAddress.get();
    }

    public StringProperty escrowAddressProperty() {
        return escrowAddress;
    }

    public void setEscrowAddress(String escrowAddress) {
        this.escrowAddress.set(escrowAddress);
    }

    public String getPaymentReference() {
        return paymentReference.get();
    }

    public StringProperty paymentReferenceProperty() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference.set(paymentReference);
    }

    public String getPayoutTxSignature() {
        return payoutTxSignature.get();
    }

    public StringProperty payoutTxSignatureProperty() {
        return payoutTxSignature;
    }

    public void setPayoutTxSignature(String payoutTxSignature) {
        this.payoutTxSignature.set(payoutTxSignature);
    }
}
