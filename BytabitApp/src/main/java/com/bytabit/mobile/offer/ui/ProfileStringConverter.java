package com.bytabit.mobile.offer.ui;

import com.bytabit.mobile.profile.model.Profile;
import javafx.util.StringConverter;

public class ProfileStringConverter extends StringConverter<Profile> {

    @Override
    public String toString(Profile profile) {
        return profile.getUserName();
    }

    @Override
    public Profile fromString(String displayName) {
        return null;
    }
}
