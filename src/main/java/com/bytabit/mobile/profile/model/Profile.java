package com.bytabit.mobile.profile.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Profile {

    public Profile() {
    }

    public Profile(String pubkey, Boolean isArbitrator, String userName, String phoneNum) {
        setPubKey(pubkey);
        setIsArbitrator(isArbitrator);
        setUserName(userName);
        setPhoneNum(phoneNum);
    }

    private final StringProperty pubKey = new SimpleStringProperty();
    private final BooleanProperty isArbitrator = new SimpleBooleanProperty();
    private final StringProperty userName = new SimpleStringProperty();
    private final StringProperty phoneNum = new SimpleStringProperty();

    public String getPubKey() {
        return pubKey.get();
    }

    public StringProperty pubKeyProperty() {
        return pubKey;
    }

    public void setPubKey(String pubKey) {
        this.pubKey.set(pubKey);
    }

    public Boolean isIsArbitrator() {
        return isArbitrator.get();
    }

    public BooleanProperty isArbitratorProperty() {
        return isArbitrator;
    }

    public void setIsArbitrator(Boolean isArbitrator) {
        this.isArbitrator.set(isArbitrator);
    }

    public String getUserName() {
        return userName.get();
    }

    public StringProperty userNameProperty() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName.set(userName);
    }

    public String getPhoneNum() {
        return phoneNum.get();
    }

    public StringProperty phoneNumProperty() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum.set(phoneNum);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Profile{");
        sb.append("pubKey=").append(pubKey);
        sb.append(", isArbitrator=").append(isArbitrator);
        sb.append(", userName=").append(userName);
        sb.append(", phoneNum=").append(phoneNum);
        sb.append('}');
        return sb.toString();
    }
}
