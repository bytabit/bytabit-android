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
        if (paymentMethod != that.paymentMethod) return false;
        return paymentDetails != null ? paymentDetails.equals(that.paymentDetails) : that.paymentDetails == null;
    }

    @Override
    public int hashCode() {
        int result = currencyCode != null ? currencyCode.hashCode() : 0;
        result = 31 * result + (paymentMethod != null ? paymentMethod.hashCode() : 0);
        result = 31 * result + (paymentDetails != null ? paymentDetails.hashCode() : 0);
        return result;
    }
}
