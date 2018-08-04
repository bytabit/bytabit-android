package com.bytabit.mobile.wallet.model;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;

public class TransactionWithAmtBuilder {

    private Transaction tx;
    private Coin transactionAmt;
    private String outputAddress;
    private String inputTxHash;
    private Coin walletBalance;
    private String escrowAddress;

    public TransactionWithAmtBuilder tx(Transaction tx) {
        this.tx = tx;
        return this;
    }

    public TransactionWithAmtBuilder transactionAmt(Coin transactionAmt) {
        this.transactionAmt = transactionAmt;
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

    public TransactionWithAmtBuilder walletBalance(Coin walletBalance) {
        this.walletBalance = walletBalance;
        return this;
    }

    public TransactionWithAmtBuilder escrowAddress(String escrowAddress) {
        this.escrowAddress = escrowAddress;
        return this;
    }

    public TransactionWithAmt build() {
        return new TransactionWithAmt(tx, transactionAmt, outputAddress, inputTxHash, walletBalance, escrowAddress);
    }
}