package com.bytabit.mobile.trade.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
public class BuyRequest {

    private final String buyerEscrowPubKey;
    private final BigDecimal btcAmount;
    private final String buyerProfilePubKey;
    private final String buyerPayoutAddress;
}
