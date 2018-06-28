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

    public PaymentDetailsKey key() {
        return new PaymentDetailsKey(this.getCurrencyCode(), this.getPaymentMethod());
    }

    public static PaymentDetailsBuilder builder() {
        return new PaymentDetailsBuilder();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PaymentDetails{");
        sb.append("currencyCode=").append(currencyCode);
        sb.append(", paymentMethod=").append(paymentMethod);
        sb.append(", paymentDetails='").append(paymentDetails).append('\'');
        sb.append('}');
        return sb.toString();
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
        int result = currencyCode.hashCode();
        result = 31 * result + paymentMethod.hashCode();
        result = 31 * result + (paymentDetails != null ? paymentDetails.hashCode() : 0);
        return result;
    }
}
