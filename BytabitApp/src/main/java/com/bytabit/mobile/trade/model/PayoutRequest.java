package com.bytabit.mobile.trade.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class PayoutRequest {

    private String paymentReference;
    private String payoutTxSignature;
}
