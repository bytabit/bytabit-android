package com.bytabit.mobile.trade.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
public class PayoutRequest {

    private String paymentReference;
    private String payoutTxSignature;
}
