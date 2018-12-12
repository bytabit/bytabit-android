package com.bytabit.mobile.wallet.model;

import lombok.*;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Getter
@EqualsAndHashCode(of = "hash")
@ToString(exclude = "transaction")
public class TransactionWithAmt {

    private Transaction transaction;

    private String hash;

    private String confidenceType;

    private Integer depth;

    private ZonedDateTime date;

    private String memo;

    private Coin transactionAmt;

    private String outputAddress;

    private String inputTxHash;

    private Coin walletBalance;

    private String escrowAddress;

    @Builder
    TransactionWithAmt(@NonNull Transaction tx, @NonNull Coin transactionAmt,
                       String outputAddress, String inputTxHash,
                       @NonNull Coin walletBalance, @NonNull String escrowAddress) {

        this.transaction = tx;
        this.hash = tx.getHashAsString();
        this.confidenceType = tx.getConfidence().getConfidenceType().name();
        this.depth = tx.getConfidence().getDepthInBlocks();
        this.date = ZonedDateTime.ofInstant(tx.getUpdateTime().toInstant(), ZoneId.systemDefault());
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
