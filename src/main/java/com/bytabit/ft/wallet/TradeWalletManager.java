package com.bytabit.ft.wallet;

import com.bytabit.ft.config.AppConfig;
import com.bytabit.ft.wallet.evt.WalletPurpose;

public class TradeWalletManager extends WalletManager {

    public TradeWalletManager() {
        super(AppConfig.getConfigName(), WalletPurpose.TRADE);
    }
}
