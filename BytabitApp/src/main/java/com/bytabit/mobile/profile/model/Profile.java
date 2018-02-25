package com.bytabit.mobile.profile.model;

public class Profile {

    private String pubKey;
    private Boolean isArbitrator;
    private String userName;
    private String phoneNum;

    public Profile() {
    }

    public Profile(String pubKey, Boolean isArbitrator, String userName, String phoneNum) {
        this.pubKey = pubKey;
        this.isArbitrator = isArbitrator;
        this.userName = userName;
        this.phoneNum = phoneNum;
    }

    public String getPubKey() {
        return pubKey;
    }

    public Boolean getIsArbitrator() {
        return isArbitrator;
    }

    public String getUserName() {
        return userName;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public static ProfileBuilder builder() {
        return new ProfileBuilder();
    }
}
