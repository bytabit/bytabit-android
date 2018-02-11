package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.common.AbstractResult;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;

import static com.bytabit.mobile.wallet.manager.TransactionResult.Type.ERROR;
import static com.bytabit.mobile.wallet.manager.TransactionResult.Type.UPDATED;

public class TransactionResult extends AbstractResult<TransactionResult.Type> {

    public enum Type {
        UPDATED, ERROR
    }

    private TransactionWithAmt transactionWithAmt;

    static TransactionResult updated(TransactionWithAmt txWithAmt) {
        return new TransactionResult(UPDATED, txWithAmt, null);
    }

    static TransactionResult error(Throwable throwable) {
        return new TransactionResult(ERROR, null, throwable);
    }

    private TransactionResult(Type type, TransactionWithAmt txWithAmt, Throwable throwable) {
        super(type, throwable);
        this.transactionWithAmt = txWithAmt;
    }

    public TransactionWithAmt getTransactionWithAmt() {
        return transactionWithAmt;
    }
}
