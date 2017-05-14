package com.bytabit.mobile.wallet;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.wallet.evt.WalletPurpose;
import javafx.beans.property.StringProperty;

public class TradeWalletManager extends WalletManager {

    public TradeWalletManager() {
        super(AppConfig.getConfigName(), WalletPurpose.TRADE);
    }

    public StringProperty balanceProperty() {
        return balance;
    }

}
