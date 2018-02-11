package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.AbstractResult;
import com.bytabit.mobile.profile.model.Profile;

import static com.bytabit.mobile.profile.manager.ProfileResult.Type.*;

public class ProfileResult extends AbstractResult<ProfileResult.Type> {

    public enum Type {
        PENDING, CREATED, LOADED, UPDATED, ERROR
    }

    private Profile profile;

    static ProfileResult pending() {
        return new ProfileResult(PENDING, null, null);
    }

    static ProfileResult created(Profile profile) {
        return new ProfileResult(CREATED, profile, null);
    }

    static ProfileResult loaded(Profile profile) {
        return new ProfileResult(LOADED, profile, null);
    }

    static ProfileResult updated(Profile profile) {
        return new ProfileResult(UPDATED, profile, null);
    }

    static ProfileResult error(Throwable error) {
        return new ProfileResult(ERROR, null, error);
    }

    private ProfileResult(Type type, Profile profile, Throwable error) {
        super(type, error);
        this.profile = profile;
    }

    public Profile getProfile() {
        return profile;
    }
}
