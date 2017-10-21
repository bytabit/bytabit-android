package com.bytabit.mobile.trade.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
public class ArbitrateRequest {

    public enum Reason {
        NO_PAYMENT, NO_BTC
    }

    private Reason reason;
}
