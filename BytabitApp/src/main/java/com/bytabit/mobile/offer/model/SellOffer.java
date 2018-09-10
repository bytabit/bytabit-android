package com.bytabit.mobile.offer.model;

import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;

import java.math.BigDecimal;

public class SellOffer {

    private String sellerEscrowPubKey;
    private String sellerProfilePubKey;
    private String arbitratorProfilePubKey;

    private CurrencyCode currencyCode;
    private PaymentMethod paymentMethod;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal price;

    public SellOffer() {
    }

    public static SellOfferBuilder builder() {
        return new SellOfferBuilder();
    }

    SellOffer(String sellerEscrowPubKey, String sellerProfilePubKey, String arbitratorProfilePubKey, CurrencyCode currencyCode,
              PaymentMethod paymentMethod, BigDecimal minAmount, BigDecimal maxAmount, BigDecimal price) {

        this.sellerEscrowPubKey = sellerEscrowPubKey;
        this.sellerProfilePubKey = sellerProfilePubKey;
        this.arbitratorProfilePubKey = arbitratorProfilePubKey;
        this.currencyCode = currencyCode;
        this.paymentMethod = paymentMethod;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.price = price;
    }

    public String getSellerEscrowPubKey() {
        return sellerEscrowPubKey;
    }

    public void setSellerEscrowPubKey(String sellerEscrowPubKey) {
        this.sellerEscrowPubKey = sellerEscrowPubKey;
    }

    public String getSellerProfilePubKey() {
        return sellerProfilePubKey;
    }

    public void setSellerProfilePubKey(String sellerProfilePubKey) {
        this.sellerProfilePubKey = sellerProfilePubKey;
    }

    public String getArbitratorProfilePubKey() {
        return arbitratorProfilePubKey;
    }

    public void setArbitratorProfilePubKey(String arbitratorProfilePubKey) {
        this.arbitratorProfilePubKey = arbitratorProfilePubKey;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean isComplete() {
        return getSellerEscrowPubKey() != null && getSellerProfilePubKey() != null &&
                getArbitratorProfilePubKey() != null &&
                getMinAmount() != null && getMaxAmount() != null &&
                getCurrencyCode() != null && getPaymentMethod() != null &&
                getPrice() != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SellOffer sellOffer = (SellOffer) o;

        return sellerEscrowPubKey.equals(sellOffer.sellerEscrowPubKey);
    }

    @Override
    public int hashCode() {
        return sellerEscrowPubKey.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SellOffer{");
        sb.append("sellerEscrowPubKey='").append(sellerEscrowPubKey).append('\'');
        sb.append(", sellerProfilePubKey='").append(sellerProfilePubKey).append('\'');
        sb.append(", arbitratorProfilePubKey='").append(arbitratorProfilePubKey).append('\'');
        sb.append(", currencyCode=").append(currencyCode);
        sb.append(", paymentMethod=").append(paymentMethod);
        sb.append(", minAmount=").append(minAmount);
        sb.append(", maxAmount=").append(maxAmount);
        sb.append(", price=").append(price);
        sb.append('}');
        return sb.toString();
    }
}
