/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.app.ui.common;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.bytabit.app.core.payment.model.CurrencyCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyEditText extends AppCompatEditText {

    private final static String REPLACEABLE = "[[a-z][A-Z],\\s]";

    private String postfix = "";
    private int maxLength = 20;
    private int maxDecimalDigits = 2;

    private CurrencyTextWatcher currencyTextWatcher;

    public CurrencyEditText(Context context) {
        this(context, null);
    }

    public CurrencyEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.appcompat.R.attr.editTextStyle);
    }

    public CurrencyEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        this.setHint(postfix);
        this.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        currencyTextWatcher = new CurrencyTextWatcher(this);
    }

    public void setCurrencyCode(@NonNull CurrencyCode currencyCode) {
        this.postfix = String.format(" %s", currencyCode.toString());
        this.maxDecimalDigits = currencyCode.getMaxTradeAmount().precision();
        this.maxDecimalDigits = currencyCode.getScale();
        this.setHint(postfix);
    }

    public BigDecimal getBigDecimalAmount() {
        if (getText() != null) {
            String amountStr = getText().toString().replaceAll(REPLACEABLE, "");
            if (!amountStr.isEmpty()) {
                return new BigDecimal(amountStr).setScale(maxDecimalDigits, BigDecimal.ROUND_HALF_UP);
            }
        }
        return BigDecimal.ZERO;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            this.addTextChangedListener(currencyTextWatcher);
        } else {
            this.removeTextChangedListener(currencyTextWatcher);
        }
        handleCaseCurrencyEmpty(focused);
    }

    /**
     * When currency empty <br/>
     * + When focus EditText, set the default text = postfix (ex: EUR) <br/>
     * + When EditText lose focus, set the default text = "", EditText will display hint (ex:EUR)
     */
    private void handleCaseCurrencyEmpty(boolean focused) {
        if (focused) {
            if (getText().toString().isEmpty()) {
                setText(postfix);
            }
        } else {
            if (getText().toString().equals(postfix)) {
                setText("");
            }
        }
    }

    private class CurrencyTextWatcher implements TextWatcher {
        private final EditText editText;
        private String previousNumber;
        DecimalFormat integerFormatter;

        /**
         * I always use locale US instead of default to make DecimalFormat work well in all language
         */
        CurrencyTextWatcher(EditText editText) {
            this.editText = editText;
            integerFormatter = new DecimalFormat("#,###.###", new DecimalFormatSymbols(Locale.US));
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // do nothing
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // do nothing
        }

        @Override
        public void afterTextChanged(Editable editable) {
            String str = editable.toString();
            if (str.length() < postfix.length()) {
                editText.setText(postfix);
                editText.setSelection(0);
                return;
            }
            if (str.equals(postfix)) {
                return;
            }

            String number = str.replace(postfix, "").replaceAll(REPLACEABLE, "");

            // for prevent afterTextChanged recursive call
            if (number.equals(previousNumber) || number.isEmpty()) {
                return;
            }
            previousNumber = number;

            String formattedString = formatNumber(number) + postfix;
            editText.removeTextChangedListener(this); // Remove listener
            editText.setText(formattedString);
            handleSelection();
            editText.addTextChangedListener(this); // Add back the listener
        }

        private String formatNumber(String number) {
            if (number.contains(".")) {
                return formatDecimal(number);
            }
            return formatInteger(number);
        }

        private String formatInteger(String str) {
            BigDecimal parsed = new BigDecimal(str);
            return integerFormatter.format(parsed);
        }

        private String formatDecimal(String str) {
            if (str.equals(".")) {
                return ".";
            }
            BigDecimal parsed = new BigDecimal(str);
            // example pattern EUR #,###.00
            DecimalFormat formatter = new DecimalFormat("#,###." + getDecimalPattern(str),
                    new DecimalFormatSymbols(Locale.US));
            formatter.setRoundingMode(RoundingMode.DOWN);
            return formatter.format(parsed);
        }

        /**
         * It will return suitable pattern for format decimal
         * For example: 10.2 -> return 0 | 10.23 -> return 00, | 10.235 -> return 000
         */
        private String getDecimalPattern(String str) {
            int decimalCount = str.length() - str.indexOf(".") - 1;
            StringBuilder decimalPattern = new StringBuilder();
            for (int i = 0; i < decimalCount && i < maxDecimalDigits; i++) {
                decimalPattern.append("0");
            }
            return decimalPattern.toString();
        }

        private void handleSelection() {
            if (editText.getText().length() <= maxLength) {
                editText.setSelection(editText.getText().length() - postfix.length());
            } else {
                editText.setSelection(maxLength - postfix.length());
            }
        }
    }
}

