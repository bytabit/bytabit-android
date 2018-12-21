package com.bytabit.mobile.trade.model;

import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class TradeRequest {

    @NonNull
    private String takerProfilePubKey;

    @NonNull
    private String takerEscrowPubKey;

    @NonNull
    private BigDecimal btcAmount;

    @NonNull
    private BigDecimal paymentAmount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TradeRequest that = (TradeRequest) o;

        if (!takerProfilePubKey.equals(that.takerProfilePubKey)) return false;
        if (!takerEscrowPubKey.equals(that.takerEscrowPubKey)) return false;
        if (btcAmount.compareTo(that.btcAmount) != 0) return false;
        return paymentAmount.compareTo(that.paymentAmount) == 0;
    }

    @Override
    public int hashCode() {
        int result = takerProfilePubKey.hashCode();
        result = 31 * result + takerEscrowPubKey.hashCode();
        result = 31 * result + btcAmount.stripTrailingZeros().hashCode();
        result = 31 * result + paymentAmount.stripTrailingZeros().hashCode();
        return result;
    }
}
