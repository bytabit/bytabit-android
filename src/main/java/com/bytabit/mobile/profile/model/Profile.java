package com.bytabit.mobile.profile.model;

public class Profile {

    public Profile(String pubKey, Boolean isArbitrator, String name, String phoneNum) {
        this.pubKey = pubKey;
        this.isArbitrator = isArbitrator;
        this.name = name;
        this.phoneNum = phoneNum;
    }

    private String pubKey;
    private Boolean isArbitrator;
    private String name;
    private String phoneNum;

    public String getPubKey() {
        return pubKey;
    }

    public Boolean getIsArbitrator() {
        return isArbitrator;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPubKey(String pubKey) {
        this.pubKey = pubKey;
    }

    public void setIsArbitrator(Boolean arbitrator) {
        isArbitrator = arbitrator;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }
}
