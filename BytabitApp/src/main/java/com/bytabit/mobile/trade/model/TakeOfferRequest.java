package com.bytabit.mobile.trade.model;

import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class TakeOfferRequest {

    @NonNull
    private String takerProfilePubKey;

    @NonNull
    private String takerEscrowPubKey;

    @NonNull
    private BigDecimal btcAmount;

    @NonNull
    private BigDecimal paymentAmount;
}
