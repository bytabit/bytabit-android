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

package com.bytabit.mobile.wallet.model;

import lombok.*;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;

import java.io.File;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class WalletKitConfig {

    private final NetworkParameters netParams;
    private final String filePrefix;
    private final File directory;

    private final List<String> mnemonicCode;
    private final Date creationDate;
    private final List<Address> watchAddresses;

    public Long getCreationTimeSeconds() {
        // determine reset wallet creation time
        Long creationTimeSeconds = null;
        if (getCreationDate() != null) {
            creationTimeSeconds = (getCreationDate().getTime());
        }
        return creationTimeSeconds;
    }
}
