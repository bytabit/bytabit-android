package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.AbstractResult;
import com.bytabit.mobile.profile.model.PaymentDetails;

import static com.bytabit.mobile.profile.manager.PaymentDetailsResult.Type.*;

public class PaymentDetailsResult extends AbstractResult<PaymentDetailsResult.Type, PaymentDetails> {

    public enum Type {
        PENDING, LOADED, UPDATED, ERROR
    }

    static PaymentDetailsResult pending() {
        return new PaymentDetailsResult(PENDING, null, null);
    }

    static PaymentDetailsResult loaded(PaymentDetails paymentDetails) {
        return new PaymentDetailsResult(LOADED, paymentDetails, null);
    }

    static PaymentDetailsResult updated(PaymentDetails paymentDetails) {
        return new PaymentDetailsResult(UPDATED, paymentDetails, null);
    }

    static PaymentDetailsResult error(Throwable error) {
        return new PaymentDetailsResult(ERROR, null, error);
    }

    private PaymentDetailsResult(Type type, PaymentDetails data, Throwable error) {
        super(type, data, error);
    }
}
