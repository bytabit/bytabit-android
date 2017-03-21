package com.bytabit.mobile.offer.model;

import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;

public class Offer {

    public Offer() {
        pubKey = new SimpleStringProperty();
        sellerPubKey = new SimpleStringProperty();
        currencyCode = new SimpleObjectProperty<>();
        paymentMethod = new SimpleObjectProperty<>();
        minAmount = new SimpleObjectProperty<>();
        maxAmount = new SimpleObjectProperty<>();
        price = new SimpleObjectProperty<>();
    }

    private final StringProperty pubKey;
    private final StringProperty sellerPubKey;
    private final ObjectProperty<CurrencyCode> currencyCode;
    private final ObjectProperty<PaymentMethod> paymentMethod;
    private final ObjectProperty<BigDecimal> minAmount;
    private final ObjectProperty<BigDecimal> maxAmount;
    private final ObjectProperty<BigDecimal> price;

    public String getPubKey() {
        return pubKey.get();
    }

    public StringProperty pubKeyProperty() {
        return pubKey;
    }

    public void setPubKey(String pubKey) {
        this.pubKey.set(pubKey);
    }

    public String getSellerPubKey() {
        return sellerPubKey.get();
    }

    public StringProperty sellerPubKeyProperty() {
        return sellerPubKey;
    }

    public void setSellerPubKey(String sellerPubKey) {
        this.sellerPubKey.set(sellerPubKey);
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
        return getPubKey() != null && getSellerPubKey() != null &&
                getMinAmount() != null && getMaxAmount() != null &&
                getCurrencyCode() != null && getPaymentMethod() != null &&
                getPrice() != null;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Offer{");
        sb.append("pubKey=").append(pubKey.get());
        sb.append(", sellerPubKey=").append(sellerPubKey.get());
        sb.append(", currencyCode=").append(currencyCode.get());
        sb.append(", paymentMethod=").append(paymentMethod.get());
        sb.append(", minAmount=").append(minAmount.get());
        sb.append(", maxAmount=").append(maxAmount.get());
        sb.append(", price=").append(price.get());
        sb.append('}');
        return sb.toString();
    }
}
