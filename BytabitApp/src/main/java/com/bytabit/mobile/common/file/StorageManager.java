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

package com.bytabit.mobile.common.file;

import com.bytabit.mobile.config.AppConfig;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;

import java.util.Optional;

public class StorageManager {

    private final String prefix = AppConfig.getBtcNetwork() + "." + AppConfig.getConfigName() + ".";

    public Optional<String> retrieve(String key) {
        return Services.get(SettingsService.class).map(s -> s.retrieve(prefix + key));
    }

    public void store(String key, String value) {
        if (value != null) {
            Services.get(SettingsService.class).ifPresent(s -> s.store(prefix + key, value));
        }
    }

    public void remove(String key) {
        Services.get(SettingsService.class).ifPresent(s -> s.remove(prefix + key));
    }
}
