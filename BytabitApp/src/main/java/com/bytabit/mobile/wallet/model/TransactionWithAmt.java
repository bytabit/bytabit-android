/*
 * Copyright 2018 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
                       @NonNull Coin walletBalance, String escrowAddress) {

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
