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

    @NonNull
    private String fundingTxHash;

    @NonNull
    private String paymentDetails;

    @NonNull
    private String refundAddress;

    @NonNull
    private String refundTxSignature;
}
