package com.bytabit.mobile.trade.model;

public class ArbitrateRequest {

    public enum Reason {
        NO_PAYMENT, NO_BTC
    }

    private Reason reason;

    public ArbitrateRequest(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }


}
