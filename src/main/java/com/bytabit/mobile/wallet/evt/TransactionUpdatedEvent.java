package com.bytabit.mobile.wallet.evt;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.wallet.Wallet;

public class TransactionUpdatedEvent {

    private final Transaction tx;
    private final Coin amt;
    private final TransactionConfidence.ConfidenceType confidenceType;
    private final Integer blockDepth;

    public static TransactionUpdatedEventBuilder builder() {
        return new TransactionUpdatedEventBuilder();
    }

    public TransactionUpdatedEvent(Transaction tx, Wallet wallet) {
        this.tx = tx;
        this.amt = tx.getValue(wallet);
        this.confidenceType = tx.getConfidence().getConfidenceType();
        this.blockDepth = tx.getConfidence().getDepthInBlocks();
    }

    public Transaction getTx() {
        return tx;
    }

    public Coin getAmt() {
        return amt;
    }

    public TransactionConfidence.ConfidenceType getConfidenceType() {
        return confidenceType;
    }

    public Integer getBlockDepth() {
        return blockDepth;
    }
}
