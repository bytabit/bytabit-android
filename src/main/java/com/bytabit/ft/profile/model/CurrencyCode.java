package com.bytabit.ft.profile.model;

import java.util.Arrays;
import java.util.List;

public enum CurrencyCode {
    SEK(PaymentMethod.SWISH, PaymentMethod.MG, PaymentMethod.WU),
    USD(PaymentMethod.MG, PaymentMethod.WU);

    CurrencyCode(PaymentMethod... paymentMethods) {
        this.paymentMethods = Arrays.asList(paymentMethods);
    }

    private List<PaymentMethod> paymentMethods;

    public List<PaymentMethod> paymentMethods() {
        return paymentMethods;
    }
}
