package com.bytabit.mobile.trade.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class ArbitrateRequest {

    public enum Reason {
        NO_PAYMENT, NO_BTC
    }

    private Reason reason;
}
