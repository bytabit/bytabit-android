package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.AbstractEvent;
import com.bytabit.mobile.profile.model.PaymentDetails;

import static com.bytabit.mobile.profile.manager.PaymentDetailsAction.Type.LOAD;
import static com.bytabit.mobile.profile.manager.PaymentDetailsAction.Type.UPDATE;

public class PaymentDetailsAction extends AbstractEvent<PaymentDetailsAction.Type, PaymentDetails> {

    public enum Type {
        LOAD, UPDATE
    }

    public static PaymentDetailsAction load() {
        return new PaymentDetailsAction(LOAD, null);
    }

    public static PaymentDetailsAction update(PaymentDetails paymentDetails) {
        return new PaymentDetailsAction(UPDATE, paymentDetails);
    }

    private PaymentDetailsAction(Type type, PaymentDetails data) {
        super(type, data);
    }
}
