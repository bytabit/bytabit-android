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

import lombok.Value;

@Value
public class DojoHdAccountResponse {

    private String status;

    private String error;

    private Data data;

    @Value
    public class Data {

        private Integer balance;

        private AddressIndices unused;

        private String derivation;

        private Integer created;

        @Value
        public class AddressIndices {

            private Integer external;

            private Integer internal;
        }
    }
}