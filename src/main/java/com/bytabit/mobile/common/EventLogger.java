package com.bytabit.mobile.common;

import io.reactivex.ObservableTransformer;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventLogger {

    private Logger logger;

    public EventLogger(Class<?> clazz) {
        logger = LoggerFactory.getLogger(clazz);
    }

    public EventLogger(Logger logger) {
        this.logger = logger;
    }

    public static EventLogger of(Class<?> clazz) {
        return new EventLogger(clazz);
    }

    public static EventLogger of(Logger logger) {
        return new EventLogger(logger);
    }

    public <E extends AbstractEvent> ObservableTransformer<E, E> logEvents() {

        return results -> results.observeOn(Schedulers.io())
                .map(result -> {
                    logger.debug("Event type: {}", result.getType());
                    return result;
                });
    }

    public <E extends AbstractEvent> ObservableTransformer<E, E> logActions() {

        return results -> results.observeOn(Schedulers.io())
                .map(result -> {
                    logger.debug("Action type: {}", result.getType());
                    return result;
                });
    }

    public <R extends AbstractResult> ObservableTransformer<R, R> logResults() {

        return results -> results.observeOn(Schedulers.io())
                .map(result -> {
                    if (result.getError() != null) {
                        logger.debug("Result type: {}, Error: {}", result.getType(), result.getError().getMessage());
                    } else {
                        logger.debug("Result type: {}", result.getType());
                    }
                    return result;
                });
    }
}