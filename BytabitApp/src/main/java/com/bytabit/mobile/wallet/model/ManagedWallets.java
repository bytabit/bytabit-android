package com.bytabit.mobile.wallet.model;

import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.wallet.Wallet;

public class ManagedWallets {

    final private Wallet tradeWallet;
    final private Wallet escrowWallet;
    final private PeerGroup peerGroup;

    public ManagedWallets(Wallet tradeWallet, Wallet escrowWallet, PeerGroup peerGroup) {
        this.tradeWallet = tradeWallet;
        this.escrowWallet = escrowWallet;
        this.peerGroup = peerGroup;
    }

    public Wallet getTradeWallet() {
        return tradeWallet;
    }

    public Wallet getEscrowWallet() {
        return escrowWallet;
    }

    public PeerGroup getPeerGroup() {
        return peerGroup;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ManagedWallets{");
        sb.append("tradeWallet balance=").append(tradeWallet.getBalance());
        sb.append(", escrowWallet watched scripts=").append(escrowWallet.getWatchedScripts().size());
        sb.append(", peerGroup isRunning=").append(peerGroup.isRunning());
        sb.append('}');
        return sb.toString();
    }
}
