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

import java.math.BigDecimal;
import java.util.Locale;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class HdAccount {

    private final static int SAT_PER_BTC = 100000000;

    @NonNull
    private String id;

    @NonNull
    private Integer balance;

    public String getSatBalance() {
        return String.format(Locale.US, "%d sat", balance);
    }

    public String getBtcBalance() {
        BigDecimal btc = balance > 0 ? new BigDecimal(balance).divide(new BigDecimal(100000000)) : BigDecimal.ZERO;
        return String.format(Locale.US, "%s BTC", btc.toPlainString());
    }
}
