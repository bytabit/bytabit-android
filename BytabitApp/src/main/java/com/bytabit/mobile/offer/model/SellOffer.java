package com.bytabit.mobile.offer.model;

import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class SellOffer {

    private String sellerEscrowPubKey;
    private String sellerProfilePubKey;
    private String arbitratorProfilePubKey;

    private CurrencyCode currencyCode;
    private PaymentMethod paymentMethod;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal price;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SellOffer sellOffer = (SellOffer) o;

        if (!sellerEscrowPubKey.equals(sellOffer.sellerEscrowPubKey)) return false;
        if (!sellerProfilePubKey.equals(sellOffer.sellerProfilePubKey)) return false;
        if (!arbitratorProfilePubKey.equals(sellOffer.arbitratorProfilePubKey))
            return false;
        if (currencyCode != sellOffer.currencyCode) return false;
        if (paymentMethod != sellOffer.paymentMethod) return false;
        if (minAmount.compareTo(sellOffer.minAmount) != 0) return false;
        if (maxAmount.compareTo(sellOffer.maxAmount) != 0) return false;
        return price.compareTo(sellOffer.price) == 0;
    }

    @Override
    public int hashCode() {
        int result = sellerEscrowPubKey.hashCode();
        result = 31 * result + sellerProfilePubKey.hashCode();
        result = 31 * result + arbitratorProfilePubKey.hashCode();
        result = 31 * result + currencyCode.hashCode();
        result = 31 * result + paymentMethod.hashCode();
        result = 31 * result + minAmount.stripTrailingZeros().hashCode();
        result = 31 * result + maxAmount.stripTrailingZeros().hashCode();
        result = 31 * result + price.stripTrailingZeros().hashCode();
        return result;
    }
}
