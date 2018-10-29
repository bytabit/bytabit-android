package com.bytabit.mobile.wallet.model;

import lombok.*;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.wallet.DeterministicSeed;

import java.io.File;

@AllArgsConstructor
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class WalletKitConfig {

    private final NetworkParameters netParams;
    private final String filePrefix;
    private final File directory;

    private DeterministicSeed deterministicSeed;
}
