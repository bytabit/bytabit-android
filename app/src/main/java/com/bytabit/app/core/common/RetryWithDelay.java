/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.app.core.common;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;

public class RetryWithDelay implements Function<Flowable<? extends Throwable>, Flowable<?>> {

    private final int maxRetryCount;
    private final int retryDelay;
    private int retryCount;
    private TimeUnit timeUnit;

    public RetryWithDelay(final int maxRetryCount, final int retryDelay, final TimeUnit timeUnit) {
        this.maxRetryCount = maxRetryCount;
        this.retryDelay = retryDelay;
        this.timeUnit = timeUnit;
        this.retryCount = 0;
    }

    @Override
    public Flowable<?> apply(final Flowable<? extends Throwable> attempts) {

        return attempts.flatMap((Function<Throwable, Flowable<?>>) throwable -> {

            if (++retryCount < maxRetryCount) {
                return Flowable.timer(retryDelay, timeUnit);
            }

            return Flowable.error(throwable);
        });
    }
}