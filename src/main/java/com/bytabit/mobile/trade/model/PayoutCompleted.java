package com.bytabit.mobile.trade.model;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PayoutCompleted {

    public enum Reason {
        SELLER_BUYER_PAYOUT, ARBITRATOR_SELLER_REFUND, ARBITRATOR_BUYER_PAYOUT
    }

    public PayoutCompleted() {
    }

    private final StringProperty escrowAddress = new SimpleStringProperty();

    private final StringProperty payoutTxHash = new SimpleStringProperty();

    private final SimpleObjectProperty<Reason> reason = new SimpleObjectProperty<>();

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

    public Reason getReason() {
        return reason.get();
    }

    public SimpleObjectProperty<Reason> reasonProperty() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason.set(reason);
    }
}
