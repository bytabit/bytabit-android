package com.bytabit.mobile.trade.evt;

import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;

public class Completed extends TradeEvent {

    private PayoutCompleted payoutCompleted;

    private TransactionWithAmt payoutTransactionWithAmt;

    public Completed(String escrowAddress, Trade.Role role, PayoutCompleted payoutCompleted, TransactionWithAmt payoutTransactionWithAmt) {
        super(escrowAddress, role);
        this.payoutCompleted = payoutCompleted;
        this.payoutTransactionWithAmt = payoutTransactionWithAmt;
    }

    public PayoutCompleted getPayoutCompleted() {
        return payoutCompleted;
    }

    public TransactionWithAmt getPayoutTransactionWithAmt() {
        return payoutTransactionWithAmt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Completed completed = (Completed) o;

        if (payoutCompleted != null ? !payoutCompleted.equals(completed.payoutCompleted) : completed.payoutCompleted != null)
            return false;
        return payoutTransactionWithAmt != null ? payoutTransactionWithAmt.equals(completed.payoutTransactionWithAmt) : completed.payoutTransactionWithAmt == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (payoutCompleted != null ? payoutCompleted.hashCode() : 0);
        result = 31 * result + (payoutTransactionWithAmt != null ? payoutTransactionWithAmt.hashCode() : 0);
        return result;
    }
}
