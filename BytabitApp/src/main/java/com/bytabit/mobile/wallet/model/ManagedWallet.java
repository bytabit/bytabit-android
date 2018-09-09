package com.bytabit.mobile.wallet.model;

import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.wallet.Wallet;

public class ManagedWallet {

    final private String name;
    final private Wallet wallet;
    final private PeerGroup peerGroup;

    public ManagedWallet(String name, Wallet wallet, PeerGroup peerGroup) {
        this.name = name;
        this.wallet = wallet;
        this.peerGroup = peerGroup;
    }

    public String getName() {
        return name;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public PeerGroup getPeerGroup() {
        return peerGroup;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ManagedWallet{");
        sb.append("name=").append(name);
        sb.append(", balance=").append(wallet.getBalance());
        sb.append(", watched scripts=").append(wallet.getWatchedScripts().size());
        sb.append(", peerGroup isRunning=").append(peerGroup.isRunning());
        sb.append('}');
        return sb.toString();
    }
}
