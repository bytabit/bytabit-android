/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.mobile.common.net;


import lombok.*;
import org.bitcoinj.core.Sha256Hash;

import java.util.Date;

@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode(exclude = {"signature"})
@ToString
public class AuthToken {

    @NonNull
    private String pubKey;

    @NonNull
    private String url;

    @NonNull
    private Date validTo;

    private String signature;

    public Sha256Hash sha256Hash() {
        String idString = String.format("|%s|%s|%s|", pubKey, url, validTo);
        return Sha256Hash.of(idString.getBytes());
    }

    public AuthToken withSignature(String signature) {
        return AuthToken.builder()
                .pubKey(this.pubKey)
                .url(this.url)
                .validTo(this.validTo)
                .signature(signature)
                .build();
    }
}
