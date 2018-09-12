package com.bytabit.mobile.trade.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PaymentRequest {

    private final String fundingTxHash;
    private final String paymentDetails;
    private final String refundAddress;
    private final String refundTxSignature;
}
