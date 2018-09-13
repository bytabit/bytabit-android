package com.bytabit.mobile.trade.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class ArbitrateRequest {

    public enum Reason {
        NO_PAYMENT, NO_BTC
    }

    private Reason reason;
}
