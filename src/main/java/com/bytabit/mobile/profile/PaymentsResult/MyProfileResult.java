package com.bytabit.mobile.profile.PaymentsResult;

import com.bytabit.mobile.common.AbstractResult;
import com.bytabit.mobile.profile.model.Profile;

import static com.bytabit.mobile.profile.PaymentsResult.MyProfileResult.Type.*;

public class MyProfileResult extends AbstractResult<MyProfileResult.Type, Profile> {

    public enum Type {
        PENDING, CREATED, LOADED, UPDATED, ERROR
    }

    public static MyProfileResult pending() {
        return new MyProfileResult(PENDING, null, null);
    }

    public static MyProfileResult created(Profile profile) {
        return new MyProfileResult(CREATED, profile, null);
    }

    public static MyProfileResult loaded(Profile profile) {
        return new MyProfileResult(LOADED, profile, null);
    }

    public static MyProfileResult updated(Profile profile) {
        return new MyProfileResult(UPDATED, profile, null);
    }

    public static MyProfileResult error(Throwable error) {
        return new MyProfileResult(ERROR, null, error);
    }

    private MyProfileResult(Type type, Profile data, Throwable error) {
        super(type, data, error);
    }
}
