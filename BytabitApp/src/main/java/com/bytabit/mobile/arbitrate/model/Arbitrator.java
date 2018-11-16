package com.bytabit.mobile.arbitrate.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Arbitrator {

    @NonNull
    private String pubkey;

    @NonNull
    private String feeAddress;
}
