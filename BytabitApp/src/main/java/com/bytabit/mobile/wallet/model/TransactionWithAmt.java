package com.bytabit.mobile.wallet.model;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;
import java.math.MathContext;

public class TransactionWithAmt {

    private Transaction transaction;
    private String hash;
    private String confidenceType;
    private Integer depth;
    private LocalDateTime date;
    private String memo;
    private Coin transactionAmt;
    private String outputAddress;
    private String inputTxHash;
    private Coin walletBalance;
    private String escrowAddress;

    public TransactionWithAmt() {
    }

    public static TransactionWithAmtBuilder builder() {
        return new TransactionWithAmtBuilder();
    }

    TransactionWithAmt(Transaction tx, Coin transactionAmt, String outputAddress, String inputTxHash, Coin walletBalance) {
        this.transaction = tx;
        this.hash = tx.getHashAsString();
        this.confidenceType = tx.getConfidence().getConfidenceType().name();
        this.depth = tx.getConfidence().getDepthInBlocks();
        this.date = new LocalDateTime(tx.getUpdateTime());
        this.memo = tx.getMemo();
        this.transactionAmt = transactionAmt;
        this.outputAddress = outputAddress;
        this.inputTxHash = inputTxHash;
        this.walletBalance = walletBalance;
    }

    TransactionWithAmt(Transaction tx, Coin transactionAmt, String outputAddress, String inputTxHash, Coin walletBalance, String escrowAddress) {
        this(tx, transactionAmt, outputAddress, inputTxHash, walletBalance);
        this.escrowAddress = escrowAddress;
    }

    public String getHash() {
        return hash;
    }

    public String getConfidenceType() {
        return confidenceType;
    }

    public Integer getDepth() {
        return depth;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getMemo() {
        return memo;
    }

    public Coin getTransactionCoinAmt() {
        return transactionAmt;
    }

    public BigDecimal getTransactionBigDecimalAmt() {
        return new BigDecimal(transactionAmt.toPlainString()).setScale(8, MathContext.DECIMAL64.getRoundingMode());
    }

    public Coin getWalletCoinBalance() {
        return walletBalance;
    }

    public BigDecimal getWalletBigDecimalBalance() {
        return new BigDecimal(walletBalance.toPlainString()).setScale(8, MathContext.DECIMAL64.getRoundingMode());
    }

    public String getOutputAddress() {
        return outputAddress;
    }

    public String getInputTxHash() {
        return inputTxHash;
    }

    public String getEscrowAddress() {
        return escrowAddress;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TransactionWithAmt that = (TransactionWithAmt) o;

        return hash != null ? hash.equals(that.hash) : that.hash == null;
    }

    @Override
    public int hashCode() {
        return hash != null ? hash.hashCode() : 0;
    }
}
