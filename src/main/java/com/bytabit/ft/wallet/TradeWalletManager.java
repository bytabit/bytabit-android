package com.bytabit.ft.wallet;

import com.bytabit.ft.config.AppConfig;

public class TradeWalletManager extends WalletManager {

    public TradeWalletManager() {
        super(AppConfig.getConfigName());
    }
}
