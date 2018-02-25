package com.bytabit.mobile.trade.evt;

import com.bytabit.mobile.trade.model.Trade;

public abstract class TradeEvent {

    private String escrowAddress;

    private Trade.Role role;

    public TradeEvent(String escrowAddress, Trade.Role role) {
        this.escrowAddress = escrowAddress;
        this.role = role;
    }

    public String getEscrowAddress() {
        return escrowAddress;
    }

    public Trade.Role getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TradeEvent that = (TradeEvent) o;

        if (escrowAddress != null ? !escrowAddress.equals(that.escrowAddress) : that.escrowAddress != null)
            return false;
        return role == that.role;
    }

    @Override
    public int hashCode() {
        int result = escrowAddress != null ? escrowAddress.hashCode() : 0;
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }
}
