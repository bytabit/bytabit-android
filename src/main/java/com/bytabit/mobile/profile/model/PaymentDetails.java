package com.bytabit.mobile.profile.model;

public class PaymentDetails {

    private CurrencyCode currencyCode;
    private PaymentMethod paymentMethod;
    private String paymentDetails;

    public PaymentDetails() {
    }

    public static PaymentDetailsBuilder builder() {
        return new PaymentDetailsBuilder();
    }

    PaymentDetails(CurrencyCode currencyCode, PaymentMethod paymentMethod, String paymentDetails) {
        this.currencyCode = currencyCode;
        this.paymentMethod = paymentMethod;
        this.paymentDetails = paymentDetails;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentDetails() {
        return paymentDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentDetails that = (PaymentDetails) o;

        if (currencyCode != that.currencyCode) return false;
        return paymentMethod == that.paymentMethod;
    }

    @Override
    public int hashCode() {
        int result = currencyCode.hashCode();
        result = 31 * result + paymentMethod.hashCode();
        return result;
    }
}
