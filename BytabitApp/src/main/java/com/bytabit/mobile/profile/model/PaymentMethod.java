package com.bytabit.mobile.profile.model;

public enum PaymentMethod {
    SWISH("Swish", "Full name and phone number"),
    WU("Western Union", "Full name and ID number"),
    MG("Moneygram", "Full name and ID number");

    PaymentMethod(String displayName, String requiredDetails) {
        this.displayName = displayName;
        this.requiredDetails = requiredDetails;
    }

    private String displayName;
    private String requiredDetails;

    public String displayName() {
        return displayName;
    }

    public String requiredDetails() {
        return requiredDetails;
    }
}
