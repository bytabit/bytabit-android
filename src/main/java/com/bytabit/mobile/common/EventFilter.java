package com.bytabit.mobile.common;

import java.util.Arrays;
import java.util.Set;

public class EventFilter<T> {

    private Set<T> eventTypes;

    public EventFilter(T[] eventTypes) {
        this.eventTypes.addAll(Arrays.asList(eventTypes));
    }

    public boolean in(AbstractEvent event) {
        return eventTypes.contains(event.getType());
    }
}
