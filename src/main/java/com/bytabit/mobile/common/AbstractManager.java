package com.bytabit.mobile.common;

import com.bytabit.mobile.config.AppConfig;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;

import java.util.Optional;

public abstract class AbstractManager {

    String prefix = AppConfig.getBtcNetwork() + "." + AppConfig.getConfigName() + ".";

    protected Optional<String> retrieve(String key) {
        return Services.get(SettingsService.class).map(s -> s.retrieve(prefix + key));
    }

    protected void store(String key, String value) {
        Services.get(SettingsService.class).ifPresent(s -> s.store(prefix + key, value));
    }

    protected void remove(String key) {
        Services.get(SettingsService.class).ifPresent(s -> s.remove(prefix + key));
    }
}
