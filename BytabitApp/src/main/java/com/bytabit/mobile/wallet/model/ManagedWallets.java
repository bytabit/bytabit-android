package com.bytabit.mobile.wallet.model;

import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.wallet.Wallet;

import java.util.List;

public class ManagedWallets {

    final private Wallet tradeWallet;
    final private List<Wallet> escrowWallets;
    final private PeerGroup peerGroup;

    public ManagedWallets(Wallet tradeWallet, List<Wallet> escrowWallets, PeerGroup peerGroup) {
        this.tradeWallet = tradeWallet;
        this.escrowWallets = escrowWallets;
        this.peerGroup = peerGroup;
    }

    public Wallet getTradeWallet() {
        return tradeWallet;
    }

    public List<Wallet> getEscrowWallets() {
        return escrowWallets;
    }

    public PeerGroup getPeerGroup() {
        return peerGroup;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ManagedWallets{");
        sb.append("tradeWallet balance=").append(tradeWallet.getBalance());
        sb.append(", escrowWallets size=").append(escrowWallets.size());
        sb.append(", peerGroup isRunning=").append(peerGroup.isRunning());
        sb.append('}');
        return sb.toString();
    }
}
