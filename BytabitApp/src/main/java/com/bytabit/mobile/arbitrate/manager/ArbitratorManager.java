package com.bytabit.mobile.arbitrate.manager;

import com.bytabit.mobile.arbitrate.model.Arbitrator;
import com.bytabit.mobile.config.AppConfig;

public class ArbitratorManager {

    private Arbitrator arbitrator;

    public ArbitratorManager() {

        String btcNetwork = AppConfig.getBtcNetwork();

        if (btcNetwork.equals("regtest")) {
            arbitrator = Arbitrator.builder()
                    .pubkey("cu4xcxY1dC3Zum4q6xYRXM1vHEjE4gnhL7Jx5DQ7vXyH")
                    .feeAddress("mzyUFtqhQxGtn4fsDRVAK3EUXPPjkhjgKC")
                    .build();
        } else if (btcNetwork.equals("test")) {
            arbitrator = Arbitrator.builder()
                    .pubkey("cu4xcxY1dC3Zum4q6xYRXM1vHEjE4gnhL7Jx5DQ7vXyH")
                    .feeAddress("mueVAJ928TUJwPruKPjzp2d2roPHDhsnRv")
                    .build();
        }
    }

    public Arbitrator getArbitrator() {
        return arbitrator;
    }
}
