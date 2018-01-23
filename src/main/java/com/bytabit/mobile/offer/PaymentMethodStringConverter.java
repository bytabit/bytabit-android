package com.bytabit.mobile.offer;

import com.bytabit.mobile.profile.model.PaymentMethod;
import javafx.util.StringConverter;

public class PaymentMethodStringConverter extends StringConverter<PaymentMethod> {

    @Override
    public String toString(PaymentMethod paymentMethod) {
        return paymentMethod.displayName();
    }

    @Override
    public PaymentMethod fromString(String displayName) {
        PaymentMethod found = null;
        for (PaymentMethod paymentMethod : PaymentMethod.values()) {
            if (paymentMethod.displayName().equals(displayName)) {
                found = paymentMethod;
                break;
            }
        }
        return found;
    }
}
