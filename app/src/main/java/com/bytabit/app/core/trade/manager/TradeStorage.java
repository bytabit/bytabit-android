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

package com.bytabit.app.core.trade.manager;

import com.bytabit.app.core.common.AppConfig;
import com.bytabit.app.core.common.file.EntityFileStorage;
import com.bytabit.app.core.trade.model.Trade;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TradeStorage extends EntityFileStorage<Trade> {

    @Inject
    public TradeStorage(AppConfig appConfig) {
        super(appConfig, Trade.class);
    }
}
