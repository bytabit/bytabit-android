package com.bytabit.mobile.wallet.model;

public class WalletManagerException extends RuntimeException {

    public WalletManagerException() {
    }

    public WalletManagerException(String message) {
        super(message);
    }

    public WalletManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public WalletManagerException(Throwable cause) {
        super(cause);
    }

    public WalletManagerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
