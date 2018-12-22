package com.bytabit.mobile.trade.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class CancelCompleted {

    public enum Reason {
        SELLER_CANCEL_UNFUNDED, BUYER_CANCEL_UNFUNDED, BUYER_CANCEL_FUNDED
    }

    private String payoutTxHash;

    @NonNull
    private Reason reason;
}
