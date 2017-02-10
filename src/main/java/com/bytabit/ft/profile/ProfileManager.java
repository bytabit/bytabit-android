package com.bytabit.ft.profile;

import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ProfileManager {

    private static Logger LOG = LoggerFactory.getLogger(ProfileManager.class);

    private String PROFILE_PUBKEY = "profile.pubkey";
    private String PROFILE_NAME = "profile.name";
    private String PROFILE_PHONENUM = "profile.phoneNum";

    private Optional<String> retrieve(String key) {
            return Services.get(SettingsService.class).map(s -> s.retrieve(key));
    }

    private void store(String key, String value) {
        Services.get(SettingsService.class).ifPresent(s -> s.store(key, value));
    }

    public Optional<String> getPubKey() {
        return retrieve(PROFILE_PUBKEY);
    }

    public void setPubKey(String pubKey) {
        store(PROFILE_PUBKEY, pubKey);
    }

    public Optional<String> getName() {
        return retrieve(PROFILE_NAME);
    }

    public void setName(String pubKey) {
        store(PROFILE_NAME, pubKey);
    }

    public Optional<String> getPhoneNum() {
        return retrieve(PROFILE_PHONENUM);
    }

    public void setPhoneNum(String pubKey) {
        store(PROFILE_PHONENUM, pubKey);
    }
}
