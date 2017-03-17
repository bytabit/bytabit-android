package com.bytabit.mobile.common;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.Offer;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import retrofit2.Retrofit;

import java.util.Optional;

public abstract class AbstractManager {

    protected final Retrofit retrofit;

    protected AbstractManager() {

        retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<Offer>(Offer.class))
                .build();
    }

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
