package com.bytabit.mobile.profile.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PaymentDetails {

    public PaymentDetails(CurrencyCode currencyCode, PaymentMethod paymentMethod, String paymentDetails) {
        setCurrencyCode(currencyCode);
        setPaymentMethod(paymentMethod);
        setPaymentDetails(paymentDetails);
    }

    private final ObjectProperty<CurrencyCode> currencyCode = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentMethod> paymentMethod = new SimpleObjectProperty<>();
    private final StringProperty paymentDetails = new SimpleStringProperty();

    public CurrencyCode getCurrencyCode() {
        return currencyCode.get();
    }

    public ObjectProperty<CurrencyCode> currencyCodeProperty() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode.set(currencyCode);
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod.get();
    }

    public ObjectProperty<PaymentMethod> paymentMethodProperty() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod.set(paymentMethod);
    }

    public String getPaymentDetails() {
        return paymentDetails.get();
    }

    public StringProperty paymentDetailsProperty() {
        return paymentDetails;
    }

    public void setPaymentDetails(String paymentDetails) {
        this.paymentDetails.set(paymentDetails);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PaymentDetails{");
        sb.append("currencyCode=").append(currencyCode);
        sb.append(", paymentMethod=").append(paymentMethod);
        sb.append(", paymentDetails=").append(paymentDetails);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentDetails that = (PaymentDetails) o;

        if (currencyCode != null ? !currencyCode.equals(that.currencyCode) : that.currencyCode != null)
            return false;
        if (paymentMethod != null ? !paymentMethod.equals(that.paymentMethod) : that.paymentMethod != null)
            return false;
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
