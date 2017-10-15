package com.bytabit.mobile.trade.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class PaymentRequest {

    private String fundingTxHash;
    private String paymentDetails;
    private String refundAddress;
    private String refundTxSignature;
}
