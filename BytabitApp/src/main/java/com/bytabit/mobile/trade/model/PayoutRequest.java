package com.bytabit.mobile.trade.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PayoutRequest {

    private final String paymentReference;
    private final String payoutTxSignature;
}
