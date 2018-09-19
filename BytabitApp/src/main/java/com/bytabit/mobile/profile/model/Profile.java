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

    @Builder.Default
    private boolean isArbitrator = false;

    private String userName;

    private String phoneNum;
}
