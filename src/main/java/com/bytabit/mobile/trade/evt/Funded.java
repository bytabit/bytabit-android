package com.bytabit.mobile.trade.evt;

import com.bytabit.mobile.trade.model.PaymentRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;

public class Funded extends TradeEvent {

    private PaymentRequest paymentRequest;

    private TransactionWithAmt transactionWithAmt;

    public Funded(String escrowAddress, Trade.Role role, PaymentRequest paymentRequest, TransactionWithAmt transactionWithAmt) {
        super(escrowAddress, role);
        this.paymentRequest = paymentRequest;
        this.transactionWithAmt = transactionWithAmt;
    }

    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public TransactionWithAmt getTransactionWithAmt() {
        return transactionWithAmt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Funded funded = (Funded) o;

        if (paymentRequest != null ? !paymentRequest.equals(funded.paymentRequest) : funded.paymentRequest != null)
            return false;
        return transactionWithAmt != null ? transactionWithAmt.equals(funded.transactionWithAmt) : funded.transactionWithAmt == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (paymentRequest != null ? paymentRequest.hashCode() : 0);
        result = 31 * result + (transactionWithAmt != null ? transactionWithAmt.hashCode() : 0);
        return result;
    }
}
