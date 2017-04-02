package com.bytabit.mobile.common;

import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;

import java.util.Optional;

public abstract class AbstractManager {

    protected Optional<String> retrieve(String key) {
        return Services.get(SettingsService.class).map(s -> s.retrieve(key));
    }

    protected void store(String key, String value) {
        Services.get(SettingsService.class).ifPresent(s -> s.store(key, value));
    }

    protected void remove(String key) {
        Services.get(SettingsService.class).ifPresent(s -> s.remove(key));
    }
}
