package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.common.AbstractEvent;

public class EscrowWalletAction extends AbstractEvent<EscrowWalletAction.Type> {

    private String escrowAddress;

    protected EscrowWalletAction(Type type, String escrowAddress) {
        super(type);
        this.escrowAddress = escrowAddress;
    }

    public enum Type {
        START_ALL, ADD_ESCROW, REMOVE_ESCROW
    }

    public String getEscrowAddress() {
        return escrowAddress;
    }
}
