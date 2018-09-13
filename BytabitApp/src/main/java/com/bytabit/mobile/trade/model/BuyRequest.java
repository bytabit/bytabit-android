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
public class BuyRequest {

    private String buyerEscrowPubKey;
    private BigDecimal btcAmount;
    private String buyerProfilePubKey;
    private String buyerPayoutAddress;
}
