package com.bytabit.mobile.profile.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@Getter
public class PaymentDetails {

    private CurrencyCode currencyCode;
    private PaymentMethod paymentMethod;
    private String paymentDetails;
}
