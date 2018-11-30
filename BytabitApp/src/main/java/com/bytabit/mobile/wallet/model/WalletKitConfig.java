package com.bytabit.mobile.wallet.model;

import lombok.*;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class WalletKitConfig {

    private final NetworkParameters netParams;
    private final String filePrefix;
    private final File directory;

    private final List<String> mnemonicCode;
    private final LocalDate creationDate;
    private final List<Address> watchAddresses;
}
