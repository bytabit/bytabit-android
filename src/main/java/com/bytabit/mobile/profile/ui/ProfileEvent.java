package com.bytabit.mobile.profile.ui;

import com.bytabit.mobile.common.AbstractEvent;
import com.bytabit.mobile.profile.model.Profile;

import static com.bytabit.mobile.profile.ui.ProfileEvent.Type.VIEW_NOT_SHOWING;
import static com.bytabit.mobile.profile.ui.ProfileEvent.Type.VIEW_SHOWING;

public class ProfileEvent extends AbstractEvent<ProfileEvent.Type, Profile> {

    public enum Type {
        VIEW_SHOWING, VIEW_NOT_SHOWING
    }

    static ProfileEvent viewShowing() {
        return new ProfileEvent(VIEW_SHOWING, null);
    }

    static ProfileEvent viewNotShowing(Profile profile) {
        return new ProfileEvent(VIEW_NOT_SHOWING, profile);
    }

    private ProfileEvent(Type type, Profile profile) {
        super(type, profile);
    }
}

