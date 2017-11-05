package com.bytabit.mobile.wallet.model;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;
import java.math.MathContext;

public class TransactionWithAmt {

    private String hash;
    private String confidenceType;
    private Integer depth;
    private LocalDateTime date;
    private String memo;
    private Coin coinAmt;
    private String outputAddress;
    private String inputTxHash;

    public TransactionWithAmt() {
    }

    public static TransactionWithAmtBuilder builder() {
        return new TransactionWithAmtBuilder();
    }

    TransactionWithAmt(Transaction tx, Coin coinAmt, String outputAddress, String inputTxHash) {
        this.hash = tx.getHashAsString();
        this.confidenceType = tx.getConfidence().getConfidenceType().name();
        this.depth = tx.getConfidence().getDepthInBlocks();
        this.date = new LocalDateTime(tx.getUpdateTime());
        this.memo = tx.getMemo();
        this.coinAmt = coinAmt;
        this.outputAddress = outputAddress;
        this.inputTxHash = inputTxHash;
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

    public Coin getCoinAmt() {
        return coinAmt;
    }

    public String getOutputAddress() {
        return outputAddress;
    }

    public String getInputTxHash() {
        return inputTxHash;
    }

    public BigDecimal getBtcAmt() {
        return new BigDecimal(coinAmt.toPlainString()).setScale(8, MathContext.DECIMAL64.getRoundingMode());
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
