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
@ToString(exclude = "transaction")
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

    @Builder
    TransactionWithAmt(Transaction tx, Coin transactionAmt, String outputAddress, String inputTxHash, Coin walletBalance, String escrowAddress) {

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

        this.escrowAddress = escrowAddress;
    }

    public BigDecimal getTransactionBigDecimalAmt() {
        return new BigDecimal(transactionAmt.toPlainString()).setScale(8, MathContext.DECIMAL64.getRoundingMode());
    }

    public BigDecimal getWalletBigDecimalBalance() {
        return new BigDecimal(walletBalance.toPlainString()).setScale(8, MathContext.DECIMAL64.getRoundingMode());
    }
}
