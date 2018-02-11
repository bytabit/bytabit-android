package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.AbstractEvent;
import com.bytabit.mobile.profile.model.PaymentDetails;

import static com.bytabit.mobile.profile.manager.PaymentDetailsAction.Type.LOAD;
import static com.bytabit.mobile.profile.manager.PaymentDetailsAction.Type.UPDATE;

public class PaymentDetailsAction extends AbstractEvent<PaymentDetailsAction.Type> {

    public enum Type {
        LOAD, UPDATE
    }

    private PaymentDetails paymentDetails;

    public static PaymentDetailsAction load() {
        return new PaymentDetailsAction(LOAD, null);
    }

    public static PaymentDetailsAction update(PaymentDetails paymentDetails) {
        return new PaymentDetailsAction(UPDATE, paymentDetails);
    }

    private PaymentDetailsAction(Type type, PaymentDetails paymentDetails) {
        super(type);
        this.paymentDetails = paymentDetails;
    }

    public PaymentDetails getPaymentDetails() {
        return paymentDetails;
    }
}
