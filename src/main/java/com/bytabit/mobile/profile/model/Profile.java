package com.bytabit.mobile.profile.model;

public class Profile {

    public Profile() {
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Profile profile = (Profile) o;

        if (pubKey != null ? !pubKey.equals(profile.pubKey) : profile.pubKey != null)
            return false;
        if (isArbitrator != null ? !isArbitrator.equals(profile.isArbitrator) : profile.isArbitrator != null)
            return false;
        if (name != null ? !name.equals(profile.name) : profile.name != null)
            return false;
        return phoneNum != null ? phoneNum.equals(profile.phoneNum) : profile.phoneNum == null;
    }

    @Override
    public int hashCode() {
        int result = pubKey != null ? pubKey.hashCode() : 0;
        result = 31 * result + (isArbitrator != null ? isArbitrator.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (phoneNum != null ? phoneNum.hashCode() : 0);
        return result;
    }
}
