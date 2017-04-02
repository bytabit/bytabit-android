package com.bytabit.mobile.profile.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Profile {

    public Profile() {
    }

    public Profile(String pubkey, Boolean isArbitrator, String name, String phoneNum) {
        setPubKey(pubkey);
        setIsArbitrator(isArbitrator);
        setName(name);
        setPhoneNum(phoneNum);
    }

    private final StringProperty pubKey = new SimpleStringProperty();
    private final BooleanProperty isArbitrator = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty();
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

    public boolean isIsArbitrator() {
        return isArbitrator.get();
    }

    public BooleanProperty isArbitratorProperty() {
        return isArbitrator;
    }

    public void setIsArbitrator(boolean isArbitrator) {
        this.isArbitrator.set(isArbitrator);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
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
        sb.append(", name=").append(name);
        sb.append(", phoneNum=").append(phoneNum);
        sb.append('}');
        return sb.toString();
    }
}
