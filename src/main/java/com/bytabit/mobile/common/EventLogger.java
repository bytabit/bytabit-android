package com.bytabit.mobile.common;

import io.reactivex.ObservableTransformer;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventLogger {

    private Logger logger;

    private EventLogger(Class<?> clazz) {
        logger = LoggerFactory.getLogger(clazz);
    }

    private EventLogger(Logger logger) {
        this.logger = logger;
    }

    public static EventLogger of(Class<?> clazz) {
        return new EventLogger(clazz);
    }

    public static EventLogger of(Logger logger) {
        return new EventLogger(logger);
    }

    public <E extends AbstractEvent> ObservableTransformer<E, E> logEvents() {

        return events -> events.observeOn(Schedulers.io())
                .map(event -> {
                    logger.debug("{}", displayTypeName(event));
                    return event;
                });
    }

    public <R extends AbstractResult> ObservableTransformer<R, R> logResults() {

        return results -> results.observeOn(Schedulers.io())
                .map(result -> {
                    if (result.getError() != null) {
                        logger.debug("{}, Error: {}", displayTypeName(result), result.getError().getMessage());
                    } else {
                        logger.debug("{}", displayTypeName(result));
                    }
                    return result;
                });
    }

    private String displayTypeName(AbstractEvent event) {
        String[] name = event.getType().getClass().getCanonicalName().split("\\.");
        int len = name.length;
        return String.format("%s.%s.%s", name[len - 2], name[len - 1], event.getType());
    }
}