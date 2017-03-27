package com.bytabit.mobile.trade.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;

public class BuyRequest {

    public BuyRequest() {
    }

    private final StringProperty sellerEscrowPubKey = new SimpleStringProperty();
    private final StringProperty buyerEscrowPubKey = new SimpleStringProperty();
    private final ObjectProperty<BigDecimal> btcAmount = new SimpleObjectProperty<>();
    private final StringProperty buyerProfilePubKey = new SimpleStringProperty();
    private final StringProperty buyerPayoutAddress = new SimpleStringProperty();

    public String getSellerEscrowPubKey() {
        return sellerEscrowPubKey.get();
    }

    public StringProperty sellerEscrowPubKeyProperty() {
        return sellerEscrowPubKey;
    }

    public void setSellerEscrowPubKey(String sellerEscrowPubKey) {
        this.sellerEscrowPubKey.set(sellerEscrowPubKey);
    }

    public String getBuyerEscrowPubKey() {
        return buyerEscrowPubKey.get();
    }

    public StringProperty buyerEscrowPubKeyProperty() {
        return buyerEscrowPubKey;
    }

    public void setBuyerEscrowPubKey(String buyerEscrowPubKey) {
        this.buyerEscrowPubKey.set(buyerEscrowPubKey);
    }

    public BigDecimal getBtcAmount() {
        return btcAmount.get();
    }

    public ObjectProperty<BigDecimal> btcAmountProperty() {
        return btcAmount;
    }

    public void setBtcAmount(BigDecimal btcAmount) {
        this.btcAmount.set(btcAmount);
    }

    public String getBuyerProfilePubKey() {
        return buyerProfilePubKey.get();
    }

    public StringProperty buyerProfilePubKeyProperty() {
        return buyerProfilePubKey;
    }

    public void setBuyerProfilePubKey(String buyerProfilePubKey) {
        this.buyerProfilePubKey.set(buyerProfilePubKey);
    }

    public String getBuyerPayoutAddress() {
        return buyerPayoutAddress.get();
    }

    public StringProperty buyerPayoutAddressProperty() {
        return buyerPayoutAddress;
    }

    public void setBuyerPayoutAddress(String buyerPayoutAddress) {
        this.buyerPayoutAddress.set(buyerPayoutAddress);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("BuyRequest{");
        sb.append("sellerEscrowPubKey=").append(sellerEscrowPubKey);
        sb.append(", buyerEscrowPubKey=").append(buyerEscrowPubKey);
        sb.append(", btcAmount=").append(btcAmount);
        sb.append(", buyerProfilePubKey=").append(buyerProfilePubKey);
        sb.append(", buyerPayoutAddress=").append(buyerPayoutAddress);
        sb.append('}');
        return sb.toString();
    }
}
