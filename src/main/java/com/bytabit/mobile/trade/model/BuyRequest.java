package com.bytabit.mobile.trade.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class BuyRequest {

    private String sellerEscrowPubkey;
    private String buyerEscrowPubKey;
    private BigDecimal btcAmount;
    private String buyerProfilePubKey;
    private String buyerPayoutAddress;
}
