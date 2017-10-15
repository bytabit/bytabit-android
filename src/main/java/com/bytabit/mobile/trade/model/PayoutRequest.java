package com.bytabit.mobile.trade.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class PayoutRequest {

    private String paymentReference;
    private String payoutTxSignature;
}
