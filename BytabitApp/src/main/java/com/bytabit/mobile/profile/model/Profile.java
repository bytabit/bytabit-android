package com.bytabit.mobile.profile.model;

public class Profile {

    private String pubKey;
    private boolean arbitrator = false;
    private String userName;
    private String phoneNum;

    public Profile() {
    }

    public Profile(String pubKey, Boolean isArbitrator, String userName, String phoneNum) {
        this.pubKey = pubKey;
        this.arbitrator = isArbitrator;
        this.userName = userName;
        this.phoneNum = phoneNum;
    }

    public String getPubKey() {
        return pubKey;
    }

    public boolean isArbitrator() {
        return arbitrator;
    }

    public String getUserName() {
        return userName;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPubKey(String pubKey) {
        this.pubKey = pubKey;
    }

    public void setArbitrator(boolean arbitrator) {
        this.arbitrator = arbitrator;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public static ProfileBuilder builder() {
        return new ProfileBuilder();
    }
}
