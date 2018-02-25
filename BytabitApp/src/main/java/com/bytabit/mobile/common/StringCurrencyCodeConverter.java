package com.bytabit.mobile.common;

import com.bytabit.mobile.profile.model.CurrencyCode;
import javafx.util.StringConverter;

public class StringCurrencyCodeConverter extends StringConverter<CurrencyCode> {

    @Override
    public String toString(CurrencyCode object) {
        return object != null ? object.toString() : null;
    }

    @Override
    public CurrencyCode fromString(String string) {
        try {
            return string != null && string.length() > 0 ? CurrencyCode.valueOf(string) : null;
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }
}
