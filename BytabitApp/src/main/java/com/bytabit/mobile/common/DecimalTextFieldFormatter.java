package com.bytabit.mobile.common;

import com.gluonhq.charm.glisten.control.TextField;
import javafx.scene.control.TextFormatter;

import java.text.DecimalFormat;
import java.text.ParsePosition;

public class DecimalTextFieldFormatter extends TextFormatter<TextField> {

    private static final DecimalFormat format = new DecimalFormat("#.0");

    public DecimalTextFieldFormatter() {
        super(c -> {
            if (c.getControlNewText().isEmpty()) {
                return c;
            }

            ParsePosition parsePosition = new ParsePosition(0);
            Object object = format.parse(c.getControlNewText(), parsePosition);

            if (object == null || parsePosition.getIndex() < c.getControlNewText().length()) {
                return null;
            } else {
                return c;
            }
        });
    }
}
