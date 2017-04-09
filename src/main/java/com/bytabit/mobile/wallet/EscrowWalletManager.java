package com.bytabit.mobile.wallet;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.wallet.evt.WalletPurpose;

public class EscrowWalletManager extends WalletManager {

    public EscrowWalletManager() {
        super(AppConfig.getConfigName(), WalletPurpose.ESCROW);
    }
}
