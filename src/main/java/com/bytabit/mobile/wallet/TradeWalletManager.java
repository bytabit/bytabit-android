package com.bytabit.mobile.wallet;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.wallet.evt.WalletPurpose;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

public class TradeWalletManager extends WalletManager {

    public TradeWalletManager() {
        super(AppConfig.getConfigName(), WalletPurpose.TRADE);
    }

    public ObservableList<TransactionWithAmt> getTransactions() {
        return transactions;
    }

    public StringProperty balanceProperty() {
        return balance;
    }

}
