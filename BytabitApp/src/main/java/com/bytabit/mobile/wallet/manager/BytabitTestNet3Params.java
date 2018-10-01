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
                "testnet-seed.bytabit.net"       // bytabit
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
