package com.bytabit.mobile.profile.model;

public class PaymentDetailsBuilder {

    private CurrencyCode currencyCode;
    private PaymentMethod paymentMethod;
    private String paymentDetails;

    public PaymentDetailsBuilder currencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
        return this;
    }

    public PaymentDetailsBuilder paymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public PaymentDetailsBuilder paymentDetails(String paymentDetails) {
        this.paymentDetails = paymentDetails;
        return this;
    }

    public PaymentDetails build() {
        return new PaymentDetails(currencyCode, paymentMethod, paymentDetails);
    }
}