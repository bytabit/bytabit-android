package com.bytabit.mobile.trade.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ArbitrateRequest {

    public enum Reason {
        NO_PAYMENT, NO_BTC
    }

    private final Reason reason;
}
