package com.bytabit.mobile.offer.model;

import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;

public class SellOffer {

    public SellOffer() {
    }

    private final StringProperty sellerEscrowPubKey = new SimpleStringProperty();
    private final StringProperty sellerProfilePubKey = new SimpleStringProperty();
    private final StringProperty arbitratorProfilePubKey = new SimpleStringProperty();

    private final ObjectProperty<CurrencyCode> currencyCode = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentMethod> paymentMethod = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> minAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> maxAmount = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>();

    public String getSellerEscrowPubKey() {
        return sellerEscrowPubKey.get();
    }

    public StringProperty sellerEscrowPubKeyProperty() {
        return sellerEscrowPubKey;
    }

    public void setSellerEscrowPubKey(String sellerEscrowPubKey) {
        this.sellerEscrowPubKey.set(sellerEscrowPubKey);
    }

    public String getSellerProfilePubKey() {
        return sellerProfilePubKey.get();
    }

    public StringProperty sellerProfilePubKeyProperty() {
        return sellerProfilePubKey;
    }

    public void setSellerProfilePubKey(String sellerProfilePubKey) {
        this.sellerProfilePubKey.set(sellerProfilePubKey);
    }

    public String getArbitratorProfilePubKey() {
        return arbitratorProfilePubKey.get();
    }

    public StringProperty arbitratorProfilePubKeyProperty() {
        return arbitratorProfilePubKey;
    }

    public void setArbitratorProfilePubKey(String arbitratorProfilePubKey) {
        this.arbitratorProfilePubKey.set(arbitratorProfilePubKey);
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode.get();
    }

    public ObjectProperty<CurrencyCode> currencyCodeProperty() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode.set(currencyCode);
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod.get();
    }

    public ObjectProperty<PaymentMethod> paymentMethodProperty() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod.set(paymentMethod);
    }

    public BigDecimal getMinAmount() {
        return minAmount.get();
    }

    public ObjectProperty<BigDecimal> minAmountProperty() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount.set(minAmount);
    }

    public BigDecimal getMaxAmount() {
        return maxAmount.get();
    }

    public ObjectProperty<BigDecimal> maxAmountProperty() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount.set(maxAmount);
    }

    public BigDecimal getPrice() {
        return price.get();
    }

    public ObjectProperty<BigDecimal> priceProperty() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price.set(price);
    }

    public Boolean isComplete() {
        return getSellerEscrowPubKey() != null && getSellerProfilePubKey() != null &&
                getArbitratorProfilePubKey() != null &&
                getMinAmount() != null && getMaxAmount() != null &&
                getCurrencyCode() != null && getPaymentMethod() != null &&
                getPrice() != null;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SellOffer{");
        sb.append("sellerEscrowPubKey=").append(sellerEscrowPubKey);
        sb.append(", sellerProfilePubKey=").append(sellerProfilePubKey);
        sb.append(", arbitratorProfilePubKey=").append(arbitratorProfilePubKey);
        sb.append(", currencyCode=").append(currencyCode);
        sb.append(", paymentMethod=").append(paymentMethod);
        sb.append(", minAmount=").append(minAmount);
        sb.append(", maxAmount=").append(maxAmount);
        sb.append(", price=").append(price);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SellOffer sellOffer = (SellOffer) o;

        if (sellerEscrowPubKey != null ? !sellerEscrowPubKey.equals(sellOffer.sellerEscrowPubKey) : sellOffer.sellerEscrowPubKey != null)
            return false;
        if (sellerProfilePubKey != null ? !sellerProfilePubKey.equals(sellOffer.sellerProfilePubKey) : sellOffer.sellerProfilePubKey != null)
            return false;
        if (arbitratorProfilePubKey != null ? !arbitratorProfilePubKey.equals(sellOffer.arbitratorProfilePubKey) : sellOffer.arbitratorProfilePubKey != null)
            return false;
        if (currencyCode != null ? !currencyCode.equals(sellOffer.currencyCode) : sellOffer.currencyCode != null)
            return false;
        if (paymentMethod != null ? !paymentMethod.equals(sellOffer.paymentMethod) : sellOffer.paymentMethod != null)
            return false;
        if (minAmount != null ? !minAmount.equals(sellOffer.minAmount) : sellOffer.minAmount != null)
            return false;
        if (maxAmount != null ? !maxAmount.equals(sellOffer.maxAmount) : sellOffer.maxAmount != null)
            return false;
        return price != null ? price.equals(sellOffer.price) : sellOffer.price == null;
    }

    @Override
    public int hashCode() {
        int result = sellerEscrowPubKey != null ? sellerEscrowPubKey.hashCode() : 0;
        result = 31 * result + (sellerProfilePubKey != null ? sellerProfilePubKey.hashCode() : 0);
        result = 31 * result + (arbitratorProfilePubKey != null ? arbitratorProfilePubKey.hashCode() : 0);
        result = 31 * result + (currencyCode != null ? currencyCode.hashCode() : 0);
        result = 31 * result + (paymentMethod != null ? paymentMethod.hashCode() : 0);
        result = 31 * result + (minAmount != null ? minAmount.hashCode() : 0);
        result = 31 * result + (maxAmount != null ? maxAmount.hashCode() : 0);
        result = 31 * result + (price != null ? price.hashCode() : 0);
        return result;
    }
}
