package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.wallet.model.WalletKitConfig;
import org.bitcoinj.kits.WalletAppKit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

public class BytabitWalletAppKit extends WalletAppKit {

    private final WalletKitConfig walletKitConfig;

    public BytabitWalletAppKit(WalletKitConfig walletKitConfig) {
        super(walletKitConfig.getNetParams(), walletKitConfig.getDirectory(), walletKitConfig.getFilePrefix());
        this.walletKitConfig = walletKitConfig;
    }

    public String getFilePrefix() {
        return this.filePrefix;
    }

    public WalletKitConfig getWalletKitConfig() {
        return walletKitConfig;
    }

    @Override
    protected void onSetupCompleted() {

        Path walletBackupFilePath = directory.toPath().resolve(String.format("%s.wallet.bkp", filePrefix));

        if (!walletBackupFilePath.toFile().exists()) {
            try {
                Files.copy(vWalletFile.toPath(), walletBackupFilePath, COPY_ATTRIBUTES);
            } catch (IOException ioe) {
                log.error("Could not make backup copy of {} to {}", vWalletFile.toPath(), walletBackupFilePath);
            }
        }
    }
}
