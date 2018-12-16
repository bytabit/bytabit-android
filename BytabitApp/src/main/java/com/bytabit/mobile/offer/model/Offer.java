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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Offer offer = (Offer) o;

        if (!id.equals(offer.id)) return false;
        if (offerType != offer.offerType) return false;
        if (!makerProfilePubKey.equals(offer.makerProfilePubKey)) return false;
        if (currencyCode != offer.currencyCode) return false;
        if (paymentMethod != offer.paymentMethod) return false;
        if (minAmount.compareTo(offer.minAmount) != 0) return false;
        if (maxAmount.compareTo(offer.maxAmount) != 0) return false;
        return price.compareTo(offer.price) == 0;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + offerType.hashCode();
        result = 31 * result + makerProfilePubKey.hashCode();
        result = 31 * result + currencyCode.hashCode();
        result = 31 * result + paymentMethod.hashCode();
        result = 31 * result + minAmount.stripTrailingZeros().hashCode();
        result = 31 * result + maxAmount.stripTrailingZeros().hashCode();
        result = 31 * result + price.stripTrailingZeros().hashCode();
        return result;
    }
}
