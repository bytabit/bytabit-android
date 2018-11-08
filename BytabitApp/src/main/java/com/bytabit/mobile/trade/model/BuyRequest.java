package com.bytabit.mobile.trade.model;

import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class BuyRequest {

    private String buyerEscrowPubKey;
    private BigDecimal btcAmount;
    private BigDecimal paymentAmount;
    private String buyerProfilePubKey;
    private String buyerPayoutAddress;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuyRequest that = (BuyRequest) o;

        if (!buyerEscrowPubKey.equals(that.buyerEscrowPubKey)) return false;
        if (btcAmount.compareTo(that.btcAmount) != 0) return false;
        if (paymentAmount.compareTo(that.paymentAmount) != 0) return false;
        if (!buyerProfilePubKey.equals(that.buyerProfilePubKey)) return false;
        return buyerPayoutAddress.equals(that.buyerPayoutAddress);
    }

    @Override
    public int hashCode() {
        int result = buyerEscrowPubKey.hashCode();
        result = 31 * result + btcAmount.stripTrailingZeros().hashCode();
        result = 31 * result + paymentAmount.stripTrailingZeros().hashCode();
        result = 31 * result + buyerProfilePubKey.hashCode();
        result = 31 * result + buyerPayoutAddress.hashCode();
        return result;
    }
}
