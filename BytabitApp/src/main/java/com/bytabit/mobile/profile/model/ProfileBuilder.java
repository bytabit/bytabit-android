package com.bytabit.mobile.profile.model;

public class ProfileBuilder {
    private String pubKey;
    private Boolean arbitrator = false;
    private String userName = "";
    private String phoneNum = "";

    public ProfileBuilder pubKey(String pubKey) {
        this.pubKey = pubKey;
        return this;
    }

    public ProfileBuilder arbitrator(Boolean isArbitrator) {
        this.arbitrator = isArbitrator;
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
        return new Profile(pubKey, arbitrator, userName, phoneNum);
    }
}