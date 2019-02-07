package com.bytabit.mobile.badge.model;

import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import lombok.*;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;

import java.time.ZonedDateTime;

@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Badge {

    public enum BadgeType {
        BETA_TESTER,
        OFFER_MAKER,
        DETAILS_VERIFIED
    }

    @Builder
    public Badge(@NonNull String profilePubKey, @NonNull BadgeType badgeType,
                 @NonNull ZonedDateTime validFrom, @NonNull ZonedDateTime validTo,
                 CurrencyCode currencyCode, PaymentMethod paymentMethod, String detailsHash) {

        this.profilePubKey = profilePubKey;
        this.badgeType = badgeType;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.currencyCode = currencyCode;
        this.paymentMethod = paymentMethod;
        this.detailsHash = detailsHash;
        this.id = getId();
    }

    @Getter(AccessLevel.NONE)
    private String id;

    private String profilePubKey;

    private BadgeType badgeType;

    private ZonedDateTime validFrom;

    private ZonedDateTime validTo;

    private CurrencyCode currencyCode;

    private PaymentMethod paymentMethod;

    private String detailsHash;

    // Use Hex encoded Sha256 Hash of badge parameters
    public String getId() {
        if (id == null) {
            String idString = String.format("|%s|%s|%s|%s|%s|%s|%s|",
                    profilePubKey, badgeType,
                    validFrom, validTo,
                    currencyCode, paymentMethod, detailsHash);

            id = Base58.encode(Sha256Hash.of(idString.getBytes()).getBytes());
        }
        return id;
    }
}
