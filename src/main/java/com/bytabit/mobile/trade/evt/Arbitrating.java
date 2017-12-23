package com.bytabit.mobile.trade.evt;

import com.bytabit.mobile.trade.model.ArbitrateRequest;
import com.bytabit.mobile.trade.model.Trade;

public class Arbitrating extends TradeEvent {

    private ArbitrateRequest arbitrateRequest;

    public Arbitrating(String escrowAddress, Trade.Role role, ArbitrateRequest arbitrateRequest) {
        super(escrowAddress, role);
        this.arbitrateRequest = arbitrateRequest;
    }

    public ArbitrateRequest getArbitrateRequest() {
        return arbitrateRequest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Arbitrating that = (Arbitrating) o;

        return arbitrateRequest != null ? arbitrateRequest.equals(that.arbitrateRequest) : that.arbitrateRequest == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (arbitrateRequest != null ? arbitrateRequest.hashCode() : 0);
        return result;
    }
}
