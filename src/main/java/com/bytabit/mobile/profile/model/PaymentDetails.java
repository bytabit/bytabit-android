package com.bytabit.mobile.profile.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@EqualsAndHashCode(exclude = "paymentDetails")
@Getter
@Builder
public class PaymentDetails {

    private final CurrencyCode currencyCode;
    private final PaymentMethod paymentMethod;
    private final String paymentDetails;
}
