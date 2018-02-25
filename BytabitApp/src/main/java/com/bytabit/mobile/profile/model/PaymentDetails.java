package com.bytabit.mobile.profile.model;

public class PaymentDetails {

    private final CurrencyCode currencyCode;
    private final PaymentMethod paymentMethod;
    private final String paymentDetails;

    public PaymentDetails(CurrencyCode currencyCode, PaymentMethod paymentMethod, String paymentDetails) {
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

    public static PaymentDetailsBuilder builder() {
        return new PaymentDetailsBuilder();
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
