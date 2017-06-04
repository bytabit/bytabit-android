package com.bytabit.mobile.wallet.model;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

public class TransactionWithAmt {

    public TransactionWithAmt(Transaction tx, Coin coinAmt, String outputAddress, String inputTxHash) {
        this.hash = tx.getHashAsString();
        this.confidenceType = tx.getConfidence().getConfidenceType().name();
        this.depth = tx.getConfidence().getDepthInBlocks();
        this.date = new LocalDateTime(tx.getUpdateTime());
        this.memo = tx.getMemo();
        this.coinAmt = coinAmt;
        this.outputAddress = outputAddress;
        this.inputTxHash = inputTxHash;
    }

    private String hash;
    private String confidenceType;
    private Integer depth;
    private LocalDateTime date;
    private String memo;
    private Coin coinAmt;
    private String outputAddress;
    private String inputTxHash;

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

    public BigDecimal getBtcAmt() {
        return new BigDecimal(coinAmt.toPlainString()).setScale(8);
    }

    public String getOutputAddress() {
        return outputAddress;
    }

    public String getInputTxHash() {
        return inputTxHash;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TransactionWithAmt{");
        sb.append("hash='").append(hash).append('\'');
        sb.append(", confidenceType='").append(confidenceType).append('\'');
        sb.append(", depth=").append(depth);
        sb.append(", date=").append(date);
        sb.append(", memo='").append(memo).append('\'');
        sb.append(", coinAmt=").append(coinAmt);
        sb.append(", outputAddress='").append(outputAddress).append('\'');
        sb.append(", inputTxHash='").append(inputTxHash).append('\'');
        sb.append('}');
        return sb.toString();
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
