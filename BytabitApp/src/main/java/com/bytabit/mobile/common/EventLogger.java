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

    public <E extends Event> ObservableTransformer<E, E> logEvents() {

        return events -> events.observeOn(Schedulers.io())
                .map(event -> {
                    logger.debug("{}", event.getClass().getSimpleName());
                    return event;
                });
    }

    public <R extends Result> ObservableTransformer<R, R> logResults() {

        return results -> results.observeOn(Schedulers.io())
                .map(result -> {
                    String className = result.getClass().getSimpleName();
                    if (result instanceof ErrorResult) {
                        logger.error("{}, Error: {}", className, ((ErrorResult) result).getError().getMessage());
                    } else {
                        logger.debug("{}", className);
                    }
                    return result;
                });
    }
}