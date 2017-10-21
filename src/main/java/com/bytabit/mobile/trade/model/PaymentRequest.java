package com.bytabit.mobile.trade.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
public class PaymentRequest {

    private String fundingTxHash;
    private String paymentDetails;
    private String refundAddress;
    private String refundTxSignature;
}
