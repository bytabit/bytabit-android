package com.bytabit.mobile.nav.evt;

public class QuitEvent extends NavEvent {

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.getClass().equals(obj.getClass());
    }
}
