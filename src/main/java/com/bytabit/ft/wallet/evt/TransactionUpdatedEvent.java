package com.bytabit.ft.wallet.evt;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.wallet.Wallet;

public class TransactionUpdatedEvent implements WalletEvent {

    private final Transaction tx;
    private final Coin amt;
    private final TransactionConfidence.ConfidenceType confidenceType;
    private final Integer blockDepth;

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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TransactionUpdatedEvent{");
        sb.append("tx=").append(tx);
        sb.append(", amt=").append(amt);
        sb.append(", confidenceType=").append(confidenceType);
        sb.append(", blockDepth=").append(blockDepth);
        sb.append('}');
        return sb.toString();
    }
}
