package com.bytabit.mobile.profile.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "pubKey")
@ToString
public class Profile {

    private String pubKey;

    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    @Builder.Default
    private boolean arbitrator = false;

    private String userName;

    private String phoneNum;

    public boolean getIsArbitrator() {
        return arbitrator;
    }

    public void setIsArbitrator(boolean isArbitrator) {
        this.arbitrator = isArbitrator;
    }
}
