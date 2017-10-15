package com.bytabit.mobile.wallet.evt;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.wallet.Wallet;

@Getter
@ToString
public class TransactionUpdatedEvent {

    private final Transaction tx;
    private final Coin amt;
    private final TransactionConfidence.ConfidenceType confidenceType;
    private final Integer blockDepth;

    @Builder
    public TransactionUpdatedEvent(Transaction tx, Wallet wallet) {
        this.tx = tx;
        this.amt = tx.getValue(wallet);
        this.confidenceType = tx.getConfidence().getConfidenceType();
        this.blockDepth = tx.getConfidence().getDepthInBlocks();
    }
}
