package com.bytabit.mobile.profile.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@Getter
@Setter(AccessLevel.PACKAGE)
public class Profile {

    private String pubKey;
    private Boolean isArbitrator;
    private String userName;
    private String phoneNum;
}
