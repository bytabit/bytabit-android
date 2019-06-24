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

package com.bytabit.app.core.arbitrate.manager;


import com.bytabit.app.core.arbitrate.model.Arbitrator;
import com.bytabit.app.core.common.AppConfig;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ArbitratorManager {

    private Arbitrator arbitrator;

    @Inject
    public ArbitratorManager(AppConfig appConfig) {

        String btcNetwork = appConfig.getBtcNetwork();

        if (btcNetwork.equals("regtest") || btcNetwork.equals("test")) {
            arbitrator = Arbitrator.builder()
                    .pubkey("oYgSGS7M85g2PtHYtRmt9C3XvCfGhygjKrF4Mf5pSmJj")
                    .feeAddress("n4hXbjymqyDpp5VXBMHibK7HSX3UFgPS73")
                    .build();
        }
    }

    public Arbitrator getArbitrator() {
        return arbitrator;
    }
}
