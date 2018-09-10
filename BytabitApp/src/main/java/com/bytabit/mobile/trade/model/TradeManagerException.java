package com.bytabit.mobile.trade.model;

public class TradeManagerException extends RuntimeException {

    public TradeManagerException() {
        super();
    }

    public TradeManagerException(String message) {
        super(message);
    }

    public TradeManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public TradeManagerException(Throwable cause) {
        super(cause);
    }

    protected TradeManagerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
