package com.bytabit.mobile.profile.model;

public class ProfileBuilder {
    private String pubKey;
    private Boolean isArbitrator;
    private String userName;
    private String phoneNum;

    public ProfileBuilder pubKey(String pubKey) {
        this.pubKey = pubKey;
        return this;
    }

    public ProfileBuilder isArbitrator(Boolean isArbitrator) {
        this.isArbitrator = isArbitrator;
        return this;
    }

    public ProfileBuilder userName(String userName) {
        this.userName = userName;
        return this;
    }

    public ProfileBuilder phoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
        return this;
    }

    public Profile build() {
        return new Profile(pubKey, isArbitrator, userName, phoneNum);
    }
}