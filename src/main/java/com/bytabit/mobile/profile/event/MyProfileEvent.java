package com.bytabit.mobile.profile.event;

import com.bytabit.mobile.common.AbstractEvent;
import com.bytabit.mobile.profile.model.Profile;

import static com.bytabit.mobile.profile.event.MyProfileEvent.Type.VIEW_NOT_SHOWING;
import static com.bytabit.mobile.profile.event.MyProfileEvent.Type.VIEW_SHOWING;

public class MyProfileEvent extends AbstractEvent<MyProfileEvent.Type, Profile> {

    public enum Type {
        VIEW_SHOWING, VIEW_NOT_SHOWING
    }

    public static MyProfileEvent viewShowing() {
        return new MyProfileEvent(VIEW_SHOWING, null);
    }

    public static MyProfileEvent viewNotShowing(Profile profile) {
        return new MyProfileEvent(VIEW_NOT_SHOWING, profile);
    }

    private MyProfileEvent(Type type, Profile profile) {
        super(type, profile);
    }
}

