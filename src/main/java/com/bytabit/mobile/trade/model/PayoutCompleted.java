package com.bytabit.mobile.trade.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PayoutCompleted {

    public PayoutCompleted() {
    }

    private final StringProperty escrowAddress = new SimpleStringProperty();

    private StringProperty payoutTxHash = new SimpleStringProperty();

    public String getEscrowAddress() {
        return escrowAddress.get();
    }

    public StringProperty escrowAddressProperty() {
        return escrowAddress;
    }

    public void setEscrowAddress(String escrowAddress) {
        this.escrowAddress.set(escrowAddress);
    }

    public String getPayoutTxHash() {
        return payoutTxHash.get();
    }

    public StringProperty payoutTxHashProperty() {
        return payoutTxHash;
    }

    public void setPayoutTxHash(String payoutTxHash) {
        this.payoutTxHash.set(payoutTxHash);
    }
}
