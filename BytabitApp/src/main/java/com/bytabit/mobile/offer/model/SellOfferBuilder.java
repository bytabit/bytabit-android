package com.bytabit.mobile.offer.model;

import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;

import java.math.BigDecimal;

public class SellOfferBuilder {

    private String sellerEscrowPubKey;
    private String sellerProfilePubKey;
    private String arbitratorProfilePubKey;
    private CurrencyCode currencyCode;
    private PaymentMethod paymentMethod;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal price;

    public SellOfferBuilder sellerEscrowPubKey(String sellerEscrowPubKey) {
        this.sellerEscrowPubKey = sellerEscrowPubKey;
        return this;
    }

    public SellOfferBuilder sellerProfilePubKey(String sellerProfilePubKey) {
        this.sellerProfilePubKey = sellerProfilePubKey;
        return this;
    }

    public SellOfferBuilder arbitratorProfilePubKey(String arbitratorProfilePubKey) {
        this.arbitratorProfilePubKey = arbitratorProfilePubKey;
        return this;
    }

    public SellOfferBuilder currencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
        return this;
    }

    public SellOfferBuilder paymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public SellOfferBuilder minAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
        return this;
    }

    public SellOfferBuilder maxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
        return this;
    }

    public SellOfferBuilder price(BigDecimal price) {
        this.price = price;
        return this;
    }

    public SellOffer build() {
        return new SellOffer(sellerEscrowPubKey, sellerProfilePubKey, arbitratorProfilePubKey, currencyCode, paymentMethod, minAmount, maxAmount, price);
    }
}
