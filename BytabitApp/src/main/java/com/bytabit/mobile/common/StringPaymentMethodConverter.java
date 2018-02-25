package com.bytabit.mobile.common;

import com.bytabit.mobile.profile.model.PaymentMethod;
import javafx.util.StringConverter;

public class StringPaymentMethodConverter extends StringConverter<PaymentMethod> {

    @Override
    public String toString(PaymentMethod object) {
        return object != null ? object.toString() : null;
    }

    @Override
    public PaymentMethod fromString(String string) {
        try {
            return string != null && string.length() > 0 ? PaymentMethod.valueOf(string) : null;
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }
}
