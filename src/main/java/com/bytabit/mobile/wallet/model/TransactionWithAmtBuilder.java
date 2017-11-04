package com.bytabit.mobile.wallet.model;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;

public class TransactionWithAmtBuilder {

    private Transaction tx;
    private Coin coinAmt;
    private String outputAddress;
    private String inputTxHash;

    public TransactionWithAmtBuilder tx(Transaction tx) {
        this.tx = tx;
        return this;
    }

    public TransactionWithAmtBuilder coinAmt(Coin coinAmt) {
        this.coinAmt = coinAmt;
        return this;
    }

    public TransactionWithAmtBuilder outputAddress(String outputAddress) {
        this.outputAddress = outputAddress;
        return this;
    }

    public TransactionWithAmtBuilder inputTxHash(String inputTxHash) {
        this.inputTxHash = inputTxHash;
        return this;
    }

    public TransactionWithAmt build() {
        return new TransactionWithAmt(tx, coinAmt, outputAddress, inputTxHash);
    }
}