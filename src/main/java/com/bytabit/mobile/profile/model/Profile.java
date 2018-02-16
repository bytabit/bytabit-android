package com.bytabit.mobile.profile.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    private String pubKey;
    private Boolean isArbitrator;
    private String userName;
    private String phoneNum;
}
