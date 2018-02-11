package com.bytabit.mobile.common;

public abstract class AbstractResult<T> extends AbstractEvent<T> {

    private Throwable error;

    protected AbstractResult(T type, Throwable error) {
        super(type);
        this.error = error;
    }

    public Throwable getError() {
        return error;
    }
}
