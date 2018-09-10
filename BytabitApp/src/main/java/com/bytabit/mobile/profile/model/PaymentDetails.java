package com.bytabit.mobile.profile.model;

public class PaymentDetails {

    private final CurrencyCode currencyCode;
    private final PaymentMethod paymentMethod;
    private final String details;

    public PaymentDetails(CurrencyCode currencyCode, PaymentMethod paymentMethod, String details) {
        this.currencyCode = currencyCode;
        this.paymentMethod = paymentMethod;
        this.details = details;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getDetails() {
        return details;
    }

    public PaymentDetailsKey key() {
        return new PaymentDetailsKey(this.getCurrencyCode(), this.getPaymentMethod());
    }

    public static PaymentDetailsBuilder builder() {
        return new PaymentDetailsBuilder();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PaymentDetails{");
        sb.append("currencyCode=").append(currencyCode);
        sb.append(", paymentMethod=").append(paymentMethod);
        sb.append(", details='").append(details).append('\'');
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
        return details != null ? details.equals(that.details) : that.details == null;
    }

    @Override
    public int hashCode() {
        int result = currencyCode.hashCode();
        result = 31 * result + paymentMethod.hashCode();
        result = 31 * result + (details != null ? details.hashCode() : 0);
        return result;
    }
}
