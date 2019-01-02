/*
 * Copyright 2018 Bytabit AB
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

package com.bytabit.mobile.wallet.manager;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.params.UnitTestParams;

public class BytabitTestNet3Params extends TestNet3Params {

    private static BytabitTestNet3Params instance;

    public BytabitTestNet3Params() {
        super();
        dnsSeeds = new String[]{
                "testnet-seed.bytabit.net",              // Bytabit
                "testnet-seed.bitcoin.jonasschnelli.ch", // Jonas Schnelli
                "testnet-seed.bluematt.me",              // Matt Corallo
                "testnet-seed.bitcoin.petertodd.org",    // Peter Todd
                //"testnet-seed.bitcoin.schildbach.de",    // Andreas Schildbach
                "bitcoin-testnet.bloqseeds.net"          // Bloq
        };
    }

    public static synchronized BytabitTestNet3Params get() {
        if (instance == null) {
            instance = new BytabitTestNet3Params();
        }
        return instance;
    }

    public static NetworkParameters fromID(String id) {
        if (id.equals(ID_MAINNET)) {
            return MainNetParams.get();
        } else if (id.equals(ID_TESTNET)) {
            return BytabitTestNet3Params.get();
        } else if (id.equals(ID_UNITTESTNET)) {
            return UnitTestParams.get();
        } else if (id.equals(ID_REGTEST)) {
            return RegTestParams.get();
        } else {
            return null;
        }
    }
}
