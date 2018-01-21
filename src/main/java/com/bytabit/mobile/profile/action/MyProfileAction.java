package com.bytabit.mobile.profile.action;

import com.bytabit.mobile.common.AbstractEvent;
import com.bytabit.mobile.profile.model.Profile;

import static com.bytabit.mobile.profile.action.MyProfileAction.Type.LOAD;
import static com.bytabit.mobile.profile.action.MyProfileAction.Type.UPDATE;

public class MyProfileAction extends AbstractEvent<MyProfileAction.Type, Profile> {

    public enum Type {
        LOAD, UPDATE
    }

    public static MyProfileAction load() {
        return new MyProfileAction(LOAD, null);
    }

    public static MyProfileAction update(Profile profile) {
        return new MyProfileAction(UPDATE, profile);
    }

    public MyProfileAction(Type type, Profile profile) {
        super(type, profile);
    }
}
