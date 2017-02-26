package com.bytabit.ft.wallet.evt;

public class WalletRunning extends WalletEvent {

    private final WalletPurpose walletPurpose;

    public WalletRunning(WalletPurpose walletType) {
        this.walletPurpose = walletType;
    }

    public WalletPurpose getWalletPurpose() {
        return walletPurpose;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("WalletRunning{");
        sb.append("walletPurpose=").append(walletPurpose);
        sb.append('}');
        return sb.toString();
    }
}
