package com.bytabit.mobile.trade.model;

public class TradeProtocolException extends RuntimeException {

    public TradeProtocolException() {
        super();
    }

    public TradeProtocolException(String message) {
        super(message);
    }

    public TradeProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public TradeProtocolException(Throwable cause) {
        super(cause);
    }

    protected TradeProtocolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
