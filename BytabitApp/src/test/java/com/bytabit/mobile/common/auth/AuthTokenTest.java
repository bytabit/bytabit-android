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

package com.bytabit.mobile.common.auth;

import com.bytabit.mobile.common.json.DateConverter;
import com.bytabit.mobile.common.net.AuthToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.ECKey;
import org.junit.Test;
import org.spongycastle.util.encoders.Base64;

import java.time.Instant;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

@Slf4j
public class AuthTokenTest {

    @Test
    public void testCreateAuthToken_whenValidSig_isValid() {

        ECKey ecKey = new ECKey();

        AuthToken authToken = AuthToken.builder()
                .pubKey(new String(Base64.encode(ecKey.getPubKey())))
                .url("https://regtest.bytabit.net/")
                .validTo(Date.from(Instant.parse("2020-04-11T00:00:00.00Z")))
                .build();

        assertThat(authToken).isNotNull();

        String signature = new String(Base64.encode(ecKey.sign(authToken.sha256Hash()).encodeToDER()));

        AuthToken signedAuthToken = authToken.withSignature(signature);

        assertThat(signedAuthToken).isNotNull();

        // encode

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(java.util.Date.class, new DateConverter())
                .create();

        String encodedSignedAuthToken = new String(Base64.encode(gson.toJson(signedAuthToken).getBytes()));

        log.debug("base64 size = {}", encodedSignedAuthToken.length());

        log.debug("Encoded signed auth token: {}", encodedSignedAuthToken);

        // decode

        AuthToken decodedSignedAuthToken = gson.fromJson(new String(Base64.decode(encodedSignedAuthToken)), AuthToken.class);
        log.debug("Decoded signed auth token: {}", decodedSignedAuthToken.toString());

        // validate

        ECKey pubKey = ECKey.fromPublicOnly(Base64.decode(decodedSignedAuthToken.getPubKey()));

        assertThat(pubKey.verify(decodedSignedAuthToken.sha256Hash(), ECKey.ECDSASignature.decodeFromDER(Base64.decode(decodedSignedAuthToken.getSignature()))));
    }
}
