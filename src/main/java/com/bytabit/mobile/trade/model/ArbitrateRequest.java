package com.bytabit.mobile.trade.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ArbitrateRequest {

    enum Reason {
        NO_PAYMENT, NO_BTC
    }

    public ArbitrateRequest() {
    }

    private final StringProperty arbitratorProfilePubKey = new SimpleStringProperty();
    private final StringProperty escrowAddress = new SimpleStringProperty();
    private final SimpleObjectProperty<Reason> reason = new SimpleObjectProperty<>();

    public String getArbitratorProfilePubKey() {
        return arbitratorProfilePubKey.get();
    }

    public StringProperty arbitratorProfilePubKeyProperty() {
        return arbitratorProfilePubKey;
    }

    public void setArbitratorProfilePubKey(String arbitratorProfilePubKey) {
        this.arbitratorProfilePubKey.set(arbitratorProfilePubKey);
    }

    public String getEscrowAddress() {
        return escrowAddress.get();
    }

    public StringProperty escrowAddressProperty() {
        return escrowAddress;
    }

    public void setEscrowAddress(String escrowAddress) {
        this.escrowAddress.set(escrowAddress);
    }

    public Reason getReason() {
        return reason.get();
    }

    public ObjectProperty<Reason> reasonProperty() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason.set(reason);
    }
}
