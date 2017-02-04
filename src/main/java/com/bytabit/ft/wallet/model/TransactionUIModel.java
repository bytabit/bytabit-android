package com.bytabit.ft.wallet.model;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.joda.time.LocalDateTime;

public class TransactionUIModel {

    private String hash;
    private String confidenceType;
    private Integer depth;
    private LocalDateTime date;
    private String memo;
    private Coin btcAmt;

    public TransactionUIModel(Transaction tx, Coin amt) {
        this(tx.getHashAsString(), tx.getConfidence().getConfidenceType().name(),
                tx.getConfidence().getDepthInBlocks(), new LocalDateTime(tx.getUpdateTime()), tx.getMemo(), amt);
    }

    public TransactionUIModel(String hash, String confidenceType, Integer depth,
                              LocalDateTime date, String memo, Coin btcAmt) {
        this.hash = hash;
        this.confidenceType = confidenceType;
        this.depth = depth;
        this.date = date;
        this.memo = memo;
        this.btcAmt = btcAmt;
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

    public Coin getBtcAmt() {
        return btcAmt;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TransactionUIModel{");
        sb.append("hash='").append(hash).append('\'');
        sb.append(", confidenceType='").append(confidenceType).append('\'');
        sb.append(", depth=").append(depth);
        sb.append(", date=").append(date);
        sb.append(", memo='").append(memo).append('\'');
        sb.append(", btcAmt=").append(btcAmt);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TransactionUIModel that = (TransactionUIModel) o;

        return hash != null ? hash.equals(that.hash) : that.hash == null;

    }

    @Override
    public int hashCode() {
        return hash != null ? hash.hashCode() : 0;
    }
}
