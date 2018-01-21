package com.bytabit.mobile.common;

public abstract class AbstractResult<T, D> extends AbstractEvent<T, D> {

    private Throwable error;

    protected AbstractResult(T type, D data, Throwable error) {
        super(type, data);
        this.error = error;
    }

    public Throwable getError() {
        return error;
    }
}
