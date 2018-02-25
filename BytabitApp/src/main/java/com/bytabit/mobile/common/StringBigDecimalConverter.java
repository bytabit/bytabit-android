package com.bytabit.mobile.common;

import javafx.util.StringConverter;

import java.math.BigDecimal;

public class StringBigDecimalConverter extends StringConverter<BigDecimal> {

    @Override
    public String toString(BigDecimal object) {
        return object != null ? object.toString() : null;
    }

    @Override
    public BigDecimal fromString(String string) {
        try {
            return string != null && string.length() > 0 ? new BigDecimal(string) : null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
