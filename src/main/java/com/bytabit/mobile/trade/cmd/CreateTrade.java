package com.bytabit.mobile.trade.cmd;

import com.bytabit.mobile.offer.model.SellOffer;

import java.math.BigDecimal;

public class CreateTrade {

    private SellOffer sellOffer;
    private BigDecimal buyBtcAmount;
    private String buyerEscrowPubKey;
    private String buyerProfilePubKey;
    private String buyerPayoutAddress;

    public CreateTrade(SellOffer sellOffer, BigDecimal buyBtcAmount, String buyerEscrowPubKey,
                       String buyerProfilePubKey, String buyerPayoutAddress) {
        this.sellOffer = sellOffer;
        this.buyBtcAmount = buyBtcAmount;
        this.buyerEscrowPubKey = buyerEscrowPubKey;
        this.buyerProfilePubKey = buyerProfilePubKey;
        this.buyerPayoutAddress = buyerPayoutAddress;
    }

    public SellOffer getSellOffer() {
        return sellOffer;
    }

    public BigDecimal getBuyBtcAmount() {
        return buyBtcAmount;
    }

    public String getBuyerEscrowPubKey() {
        return buyerEscrowPubKey;
    }

    public String getBuyerProfilePubKey() {
        return buyerProfilePubKey;
    }

    public String getBuyerPayoutAddress() {
        return buyerPayoutAddress;
    }
}
