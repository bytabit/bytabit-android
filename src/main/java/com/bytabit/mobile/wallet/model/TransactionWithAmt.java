package com.bytabit.mobile.wallet.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;
import java.math.MathContext;

@Getter
@EqualsAndHashCode(of = "hash")
@ToString
public class TransactionWithAmt {

    @Builder
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

    public BigDecimal getBtcAmt() {
        return new BigDecimal(coinAmt.toPlainString()).setScale(8, MathContext.DECIMAL64.getRoundingMode());
    }

}
