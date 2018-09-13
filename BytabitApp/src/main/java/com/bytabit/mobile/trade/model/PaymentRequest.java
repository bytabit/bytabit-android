package com.bytabit.mobile.trade.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class PaymentRequest {

    private String fundingTxHash;
    private String paymentDetails;
    private String refundAddress;
    private String refundTxSignature;
}
