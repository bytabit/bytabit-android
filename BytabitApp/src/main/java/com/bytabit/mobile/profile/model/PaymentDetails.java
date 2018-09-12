package com.bytabit.mobile.profile.model;

import lombok.*;

@AllArgsConstructor
@Getter
@Builder
@EqualsAndHashCode
@ToString
public class PaymentDetails {

    private final CurrencyCode currencyCode;
    private final PaymentMethod paymentMethod;
    private final String details;
}
