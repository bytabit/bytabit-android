package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.AbstractEvent;
import com.bytabit.mobile.profile.model.Profile;

public class ProfileAction extends AbstractEvent<ProfileAction.Type, Profile> {

    public enum Type {
        LOAD, UPDATE
    }

    public ProfileAction(Type type, Profile profile) {
        super(type, profile);
    }
}
