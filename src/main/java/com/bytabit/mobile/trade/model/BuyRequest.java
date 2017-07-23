package com.bytabit.mobile.trade.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;

public class BuyRequest {

    public BuyRequest() {
    }

    public BuyRequest(String escrowAddress, String sellerEscrowPubKey, String buyerEscrowPubKey,
                      BigDecimal btcAmount, String buyerProfilePubKey, String buyerPayoutAddress) {
        setEscrowAddress(escrowAddress);
        setSellerEscrowPubKey(sellerEscrowPubKey);
        setBuyerEscrowPubKey(buyerEscrowPubKey);
        setBtcAmount(btcAmount);
        setBuyerProfilePubKey(buyerProfilePubKey);
        setBuyerPayoutAddress(buyerPayoutAddress);
    }

    private final StringProperty escrowAddress = new SimpleStringProperty();
    private final StringProperty sellerEscrowPubKey = new SimpleStringProperty();
    private final StringProperty buyerEscrowPubKey = new SimpleStringProperty();
    private final ObjectProperty<BigDecimal> btcAmount = new SimpleObjectProperty<>();
    private final StringProperty buyerProfilePubKey = new SimpleStringProperty();
    private final StringProperty buyerPayoutAddress = new SimpleStringProperty();

    public String getEscrowAddress() {
        return escrowAddress.get();
    }

    public StringProperty escrowAddressProperty() {
        return escrowAddress;
    }

    public void setEscrowAddress(String escrowAddress) {
        this.escrowAddress.set(escrowAddress);
    }

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
        sb.append("escrowAddress=").append(escrowAddress);
        sb.append(", sellerEscrowPubKey=").append(sellerEscrowPubKey);
        sb.append(", buyerEscrowPubKey=").append(buyerEscrowPubKey);
        sb.append(", btcAmount=").append(btcAmount);
        sb.append(", buyerProfilePubKey=").append(buyerProfilePubKey);
        sb.append(", buyerPayoutAddress=").append(buyerPayoutAddress);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuyRequest that = (BuyRequest) o;

        if (escrowAddress != null ? !escrowAddress.equals(that.escrowAddress) : that.escrowAddress != null)
            return false;
        if (sellerEscrowPubKey != null ? !sellerEscrowPubKey.equals(that.sellerEscrowPubKey) : that.sellerEscrowPubKey != null)
            return false;
        if (buyerEscrowPubKey != null ? !buyerEscrowPubKey.equals(that.buyerEscrowPubKey) : that.buyerEscrowPubKey != null)
            return false;
        if (btcAmount != null ? !btcAmount.equals(that.btcAmount) : that.btcAmount != null)
            return false;
        if (buyerProfilePubKey != null ? !buyerProfilePubKey.equals(that.buyerProfilePubKey) : that.buyerProfilePubKey != null)
            return false;
        return buyerPayoutAddress != null ? buyerPayoutAddress.equals(that.buyerPayoutAddress) : that.buyerPayoutAddress == null;
    }

    @Override
    public int hashCode() {
        int result = escrowAddress != null ? escrowAddress.hashCode() : 0;
        result = 31 * result + (sellerEscrowPubKey != null ? sellerEscrowPubKey.hashCode() : 0);
        result = 31 * result + (buyerEscrowPubKey != null ? buyerEscrowPubKey.hashCode() : 0);
        result = 31 * result + (btcAmount != null ? btcAmount.hashCode() : 0);
        result = 31 * result + (buyerProfilePubKey != null ? buyerProfilePubKey.hashCode() : 0);
        result = 31 * result + (buyerPayoutAddress != null ? buyerPayoutAddress.hashCode() : 0);
        return result;
    }
}
