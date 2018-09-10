package com.bytabit.mobile.common;

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
