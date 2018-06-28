package com.bytabit.mobile.profile.model;

public class PaymentDetailsKey {

    private final CurrencyCode currencyCode;
    private final PaymentMethod paymentMethod;

    public PaymentDetailsKey(CurrencyCode currencyCode, PaymentMethod paymentMethod) {
        this.currencyCode = currencyCode;
        this.paymentMethod = paymentMethod;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentDetailsKey that = (PaymentDetailsKey) o;

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
