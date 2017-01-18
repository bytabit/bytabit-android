package com.bytabit.ft.wallet.model;

import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

public class TransactionUIModel {

    private String hash;
    private String confidenceType;
    private Integer depth;
    private LocalDateTime date;
    private String memo;
    private BigDecimal btcAmt;

    public TransactionUIModel(String hash, String confidenceType, Integer depth,
                              LocalDateTime date, String memo, BigDecimal btcAmt) {
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

    public BigDecimal getBtcAmt() {
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
}
