package com.bytabit.mobile.trade.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PayoutDetails {

    public PayoutDetails() {
    }

    private StringProperty payoutTxHash = new SimpleStringProperty();

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
