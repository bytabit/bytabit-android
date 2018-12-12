package com.bytabit.mobile.offer.model;

import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import lombok.*;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;

import java.math.BigDecimal;
import java.math.RoundingMode;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Offer {

    public enum OfferType {
        BUY,
        SELL
    }

    @Builder
    public Offer(@NonNull OfferType offerType, @NonNull String makerProfilePubKey,
                 @NonNull CurrencyCode currencyCode, @NonNull PaymentMethod paymentMethod,
                 @NonNull BigDecimal minAmount, @NonNull BigDecimal maxAmount,
                 @NonNull BigDecimal price) {

        this.offerType = offerType;
        this.makerProfilePubKey = makerProfilePubKey;
        this.currencyCode = currencyCode;
        this.paymentMethod = paymentMethod;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.price = price;
        this.id = getId();
    }

    @Getter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private String id;

    private OfferType offerType;

    private String makerProfilePubKey;

    private CurrencyCode currencyCode;

    private PaymentMethod paymentMethod;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private BigDecimal price;

    // Use Hex encoded Sha256 Hash of offer parameters
    public String getId() {
        if (id == null) {
            String idString = String.format("|%s|%s|%s|%s|%s|%s|%s|", offerType,
                    makerProfilePubKey, currencyCode, paymentMethod,
                    minAmount.setScale(currencyCode.getScale(), RoundingMode.HALF_UP),
                    maxAmount.setScale(currencyCode.getScale(), RoundingMode.HALF_UP),
                    price.setScale(currencyCode.getScale(), RoundingMode.HALF_UP));

            id = Base58.encode(Sha256Hash.of(idString.getBytes()).getBytes());
        }
        return id;
    }
}
