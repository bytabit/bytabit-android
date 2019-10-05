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

package com.bytabit.app.core.wallet.model;

import com.bytabit.app.core.common.file.Entity;
import com.bytabit.app.core.wallet.manager.WalletManager;

import java.time.LocalDateTime;

import lombok.NonNull;
import lombok.Value;

@Value
public class HdWallet implements Entity {

    @NonNull
    private String id;

    @NonNull
    // base64 encoded
    private String seed;

    @NonNull
    private WalletManager.SegwitDerivation segwitDerivation;

    @NonNull
    private LocalDateTime created;

    // in Satoshi
    private Integer balance;

    private Integer externalUnusedIndex;

    private Integer internalUnusedIndex;
}
