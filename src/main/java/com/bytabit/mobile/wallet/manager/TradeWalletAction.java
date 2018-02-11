package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.common.AbstractEvent;

import java.math.BigDecimal;
import java.util.List;

public class TradeWalletAction extends AbstractEvent<TradeWalletAction.Type> {

    private List<String> seedWords;

    private String withdrawAddress;

    private BigDecimal withdrawAmount;

    private BigDecimal transactionFee;

    public TradeWalletAction(Type type, List<String> seedWords, String withdrawAddress,
                             BigDecimal withdrawAmount, BigDecimal transactionFee) {
        super(type);
        this.seedWords = seedWords;
        this.withdrawAddress = withdrawAddress;
        this.withdrawAmount = withdrawAmount;
        this.transactionFee = transactionFee;
    }

    public enum Type {
        START, RESTORE, DEPOSIT, WITHDRAW
    }
}
