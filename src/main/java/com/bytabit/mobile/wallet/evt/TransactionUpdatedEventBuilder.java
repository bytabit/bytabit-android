package com.bytabit.mobile.wallet.evt;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;

public class TransactionUpdatedEventBuilder {
    private Transaction tx;
    private Wallet wallet;

    public TransactionUpdatedEventBuilder tx(Transaction tx) {
        this.tx = tx;
        return this;
    }

    public TransactionUpdatedEventBuilder wallet(Wallet wallet) {
        this.wallet = wallet;
        return this;
    }

    public TransactionUpdatedEvent build() {
        return new TransactionUpdatedEvent(tx, wallet);
    }
}