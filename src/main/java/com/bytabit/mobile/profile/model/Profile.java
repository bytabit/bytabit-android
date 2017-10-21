package com.bytabit.mobile.profile.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@Getter
@Setter(AccessLevel.PACKAGE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Profile {

    private String pubKey;
    private Boolean isArbitrator;
    private String userName;
    private String phoneNum;
}
