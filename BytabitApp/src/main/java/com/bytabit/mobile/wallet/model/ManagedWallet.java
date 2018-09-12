package com.bytabit.mobile.wallet.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.wallet.Wallet;

@AllArgsConstructor
@Getter
@Builder
@ToString
public class ManagedWallet {

    private final String name;

    private final Wallet wallet;

    private final PeerGroup peerGroup;
}
