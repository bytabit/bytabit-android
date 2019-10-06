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

package com.bytabit.app.core.wallet.dojo;


import com.google.gson.annotations.SerializedName;

import lombok.Value;

@Value
public class DojoAuthResponse {

    // "authorizations": {
    //    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJTYW1vdXJhaSBXYWxsZXQgYmFja2VuZCIsInR5cGUiOiJhY2Nlc3MtdG9rZW4iLCJwcmYiOiJhZG1pbiIsImlhdCI6MTU2OTg5NTI4NywiZXhwIjoxNTY5ODk2MTg3fQ.k6QpkY1Sp8mUtGcT67oBCHCcNhACKoRHbtGS5vtQAms",
    //    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJTYW1vdXJhaSBXYWxsZXQgYmFja2VuZCIsInR5cGUiOiJyZWZyZXNoLXRva2VuIiwicHJmIjoiYWRtaW4iLCJpYXQiOjE1Njk4OTUyODcsImV4cCI6MTU2OTkwMjQ4N30.Ow0uvc9yr2ZZjIJ334L-ZYM03sB1B-mt3nHooH1ravc"
    //  }

    private Authorizations authorizations;

    @Value
    public class Authorizations {

        @SerializedName("access_token")
        private String accessToken;

        @SerializedName("refresh_token")
        private String refreshToken;
    }
}
