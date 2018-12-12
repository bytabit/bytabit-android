package com.bytabit.mobile.trade.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class Confirmation {

    @NonNull
    private String makerEscrowPubKey;

    @NonNull
    private String arbitratorProfilePubKey;
}
