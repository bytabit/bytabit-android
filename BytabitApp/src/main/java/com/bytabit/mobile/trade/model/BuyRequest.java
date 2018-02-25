package com.bytabit.mobile.trade.model;

import java.math.BigDecimal;

public class BuyRequest {

    private String buyerEscrowPubKey;
    private BigDecimal btcAmount;
    private String buyerProfilePubKey;
    private String buyerPayoutAddress;

    public BuyRequest(String buyerEscrowPubKey, BigDecimal btcAmount, String buyerProfilePubKey, String buyerPayoutAddress) {
        this.buyerEscrowPubKey = buyerEscrowPubKey;
        this.btcAmount = btcAmount;
        this.buyerProfilePubKey = buyerProfilePubKey;
        this.buyerPayoutAddress = buyerPayoutAddress;
    }

    public String getBuyerEscrowPubKey() {
        return buyerEscrowPubKey;
    }

    public BigDecimal getBtcAmount() {
        return btcAmount;
    }

    public String getBuyerProfilePubKey() {
        return buyerProfilePubKey;
    }

    public String getBuyerPayoutAddress() {
        return buyerPayoutAddress;
    }
}
