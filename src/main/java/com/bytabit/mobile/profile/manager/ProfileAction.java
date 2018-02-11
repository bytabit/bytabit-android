package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.AbstractEvent;
import com.bytabit.mobile.profile.model.Profile;

public class ProfileAction extends AbstractEvent<ProfileAction.Type> {

    public enum Type {
        LOAD, UPDATE
    }

    private Profile profile;

    public static ProfileAction load() {
        return new ProfileAction(Type.LOAD, null);
    }

    public static ProfileAction update(Profile profile) {
        return new ProfileAction(Type.UPDATE, profile);
    }

    private ProfileAction(Type type, Profile profile) {
        super(type);
        this.profile = profile;
    }

    public Profile getProfile() {
        return profile;
    }
}
