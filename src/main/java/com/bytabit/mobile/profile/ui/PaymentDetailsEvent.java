package com.bytabit.mobile.profile.ui;

import com.bytabit.mobile.common.AbstractEvent;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;

import static com.bytabit.mobile.profile.ui.PaymentDetailsEvent.Type.*;

public class PaymentDetailsEvent extends AbstractEvent<PaymentDetailsEvent.Type, PaymentDetails> {

    public enum Type {
        LIST_VIEW_SHOWING, LIST_VIEW_NOT_SHOWING, LIST_ITEM_CHANGED,
        LIST_ADD_BUTTON_PRESSED, DETAILS_VIEW_SHOWING, DETAILS_VIEW_NOT_SHOWING,
        DETAILS_ADD_BUTTON_PRESSED, DETAILS_BACK_BUTTON_PRESSED, DETAILS_CURRENCY_SELECTED
    }

    public static PaymentDetailsEvent listViewShowing() {
        return new PaymentDetailsEvent(LIST_VIEW_SHOWING, null);
    }

    public static PaymentDetailsEvent listViewNotShowing() {
        return new PaymentDetailsEvent(LIST_VIEW_NOT_SHOWING, null);
    }

    public static PaymentDetailsEvent listItemChanged(PaymentDetails paymentDetails) {
        return new PaymentDetailsEvent(LIST_ITEM_CHANGED, paymentDetails);
    }

    public static PaymentDetailsEvent listAddButtonPressed() {
        return new PaymentDetailsEvent(LIST_ADD_BUTTON_PRESSED, null);
    }

    public static PaymentDetailsEvent detailsViewShowing() {
        return new PaymentDetailsEvent(DETAILS_VIEW_SHOWING, null);
    }

    public static PaymentDetailsEvent detailsViewNotShowing() {
        return new PaymentDetailsEvent(DETAILS_VIEW_NOT_SHOWING, null);
    }

    public static PaymentDetailsEvent detailsAddButtonPressed(PaymentDetails paymentDetails) {
        return new PaymentDetailsEvent(DETAILS_ADD_BUTTON_PRESSED, paymentDetails);
    }

    public static PaymentDetailsEvent detailsBackButtonPressed() {
        return new PaymentDetailsEvent(DETAILS_BACK_BUTTON_PRESSED, null);
    }

    public static PaymentDetailsEvent detailsCurrencySelected(CurrencyCode currencyCode) {
        return new PaymentDetailsEvent(DETAILS_CURRENCY_SELECTED,
                PaymentDetails.builder().currencyCode(currencyCode).build());
    }

    private PaymentDetailsEvent(Type type, PaymentDetails data) {
        super(type, data);
    }
}

