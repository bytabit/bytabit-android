package com.bytabit.mobile.profile.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class Profile {

    private String userName;

    private String phoneNum;
}
