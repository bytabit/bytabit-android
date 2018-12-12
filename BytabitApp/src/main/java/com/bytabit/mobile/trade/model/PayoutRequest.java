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

    @NonNull
    private String paymentReference;

    @NonNull
    private String payoutAddress;

    @NonNull
    private String payoutTxSignature;
}
