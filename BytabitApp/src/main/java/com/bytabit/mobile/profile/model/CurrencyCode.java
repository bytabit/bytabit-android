package com.bytabit.mobile.profile.model;

import java.util.Arrays;
import java.util.List;

public enum CurrencyCode {

    SEK(0, PaymentMethod.SWISH, PaymentMethod.MG, PaymentMethod.WU),
    USD(2, PaymentMethod.MG, PaymentMethod.WU);

    CurrencyCode(int scale, PaymentMethod... paymentMethods) {
        this.scale = scale;
        this.paymentMethods = Arrays.asList(paymentMethods);
    }

    private int scale;

    private List<PaymentMethod> paymentMethods;

    public int getScale() {
        return scale;
    }

    public List<PaymentMethod> paymentMethods() {
        return paymentMethods;
    }
}
