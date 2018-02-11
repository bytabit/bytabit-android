package com.bytabit.mobile.common;

import com.google.common.collect.Sets;

public abstract class AbstractEvent<T> {

    private T type;

    protected AbstractEvent(T type) {
        this.type = type;
    }

    public T getType() {
        return type;
    }

    public boolean matches(T... types) {
        return Sets.newHashSet(types).contains(type);
    }
}
