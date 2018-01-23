package com.bytabit.mobile.common;

import com.google.common.collect.Sets;

public abstract class AbstractEvent<T, D> {

    private T type;
    private D data;

    protected AbstractEvent(T type, D data) {
        this.type = type;
        this.data = data;
    }

    public T getType() {
        return type;
    }

    public D getData() {
        return data;
    }

    public boolean matches(T... types) {
        return Sets.newHashSet(types).contains(type);
    }
}
