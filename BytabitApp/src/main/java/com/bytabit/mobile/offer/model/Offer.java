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
@EqualsAndHashCode
@ToString
public class Offer {

    public enum OfferType {
        BUY,
        SELL
    }

    private OfferType offerType;
    private String traderEscrowPubKey;
    private String traderProfilePubKey;
    private String arbitratorProfilePubKey;

    private CurrencyCode currencyCode;
    private PaymentMethod paymentMethod;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal price;
}
