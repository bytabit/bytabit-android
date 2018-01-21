package com.bytabit.mobile.profile.action;

import com.bytabit.mobile.common.AbstractEvent;
import com.bytabit.mobile.profile.model.PaymentDetails;

import static com.bytabit.mobile.profile.action.PaymentDetailsAction.Type.*;

public class PaymentDetailsAction extends AbstractEvent<PaymentDetailsAction.Type, PaymentDetails> {

    public enum Type {
        LOAD, ADD, UPDATE
    }

    public static PaymentDetailsAction load() {
        return new PaymentDetailsAction(LOAD, null);
    }

    public static PaymentDetailsAction add(PaymentDetails paymentDetails) {
        return new PaymentDetailsAction(ADD, paymentDetails);
    }

    public static PaymentDetailsAction update(PaymentDetails paymentDetails) {
        return new PaymentDetailsAction(UPDATE, paymentDetails);
    }

    public PaymentDetailsAction(Type type, PaymentDetails data) {
        super(type, data);
    }
}
