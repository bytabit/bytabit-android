package com.bytabit.mobile.trade.evt;

import com.bytabit.mobile.trade.model.PayoutRequest;
import com.bytabit.mobile.trade.model.Trade;

public class Paid extends TradeEvent {

    private PayoutRequest payoutRequest;

    public Paid(String escrowAddress, Trade.Role role, PayoutRequest payoutRequest) {
        super(escrowAddress, role);
        this.payoutRequest = payoutRequest;
    }

    public PayoutRequest getPayoutRequest() {
        return payoutRequest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Paid paid = (Paid) o;

        return payoutRequest != null ? payoutRequest.equals(paid.payoutRequest) : paid.payoutRequest == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (payoutRequest != null ? payoutRequest.hashCode() : 0);
        return result;
    }
}
