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

    public boolean getIsArbitrator() {
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

    public void setIsArbitrator(boolean arbitrator) {
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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Profile{");
        sb.append("pubKey='").append(pubKey).append('\'');
        sb.append(", arbitrator=").append(arbitrator);
        sb.append(", userName='").append(userName).append('\'');
        sb.append(", phoneNum='").append(phoneNum).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Profile profile = (Profile) o;

        if (!pubKey.equals(profile.pubKey)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return pubKey.hashCode();
    }
}
