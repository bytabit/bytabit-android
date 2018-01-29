package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.common.AbstractEvent;

import java.math.BigDecimal;

public class WalletAction extends AbstractEvent<WalletAction.Type, BigDecimal> {

    public enum Type {
        DEPOSIT, WITHDRAW
    }

    public WalletAction(Type type, BigDecimal amount) {
        super(type, amount);
    }
}
