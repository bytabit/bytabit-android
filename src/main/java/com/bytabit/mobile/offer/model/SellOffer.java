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

    public static SellOfferBuilder builder() {
        return new SellOfferBuilder();
    }

    SellOffer(String sellerEscrowPubKey, String sellerProfilePubKey, String arbitratorProfilePubKey, CurrencyCode currencyCode, PaymentMethod paymentMethod, BigDecimal minAmount, BigDecimal maxAmount, BigDecimal price) {
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

        if (sellerEscrowPubKey != null ? !sellerEscrowPubKey.equals(sellOffer.sellerEscrowPubKey) : sellOffer.sellerEscrowPubKey != null)
            return false;
        if (sellerProfilePubKey != null ? !sellerProfilePubKey.equals(sellOffer.sellerProfilePubKey) : sellOffer.sellerProfilePubKey != null)
            return false;
        if (arbitratorProfilePubKey != null ? !arbitratorProfilePubKey.equals(sellOffer.arbitratorProfilePubKey) : sellOffer.arbitratorProfilePubKey != null)
            return false;
        if (currencyCode != sellOffer.currencyCode) return false;
        if (paymentMethod != sellOffer.paymentMethod) return false;
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
