package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.wallet.model.WalletKitConfig;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.wallet.DeterministicSeed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

public class BytabitWalletAppKit extends WalletAppKit {

    private final WalletKitConfig walletKitConfig;

    BytabitWalletAppKit(WalletKitConfig walletKitConfig, DeterministicSeed currentSeed) {
        super(walletKitConfig.getNetParams(), walletKitConfig.getDirectory(), walletKitConfig.getFilePrefix());
        this.walletKitConfig = walletKitConfig;

        // determine reset wallet creation time
        long resetCreationTimeSeconds;
        if (walletKitConfig.getCreationDate() != null) {
            resetCreationTimeSeconds = (walletKitConfig.getCreationDate().toEpochDay() - 1) * 24 * 60 * 60;
        } else if (currentSeed != null) {
            resetCreationTimeSeconds = currentSeed.getCreationTimeSeconds();
        } else {
            resetCreationTimeSeconds = 0L;
        }

        if (walletKitConfig.getWatchAddresses() != null && !walletKitConfig.getWatchAddresses().isEmpty()) {
            this.wallet().addWatchedAddresses(walletKitConfig.getWatchAddresses(), resetCreationTimeSeconds);
        }

        // reset wallet with new seed and creation time
        if (walletKitConfig.getMnemonicCode() != null && !walletKitConfig.getMnemonicCode().isEmpty()) {
            DeterministicSeed newSeed = new DeterministicSeed(walletKitConfig.getMnemonicCode(), null, "", resetCreationTimeSeconds);
            this.restoreWalletFromSeed(newSeed);
        }
        // else reset wallet with same seed and new creation time
        else if (currentSeed != null) {
            currentSeed.setCreationTimeSeconds(resetCreationTimeSeconds);
            this.restoreWalletFromSeed(currentSeed);
        }

        // if regtest connect to localhost
        if (walletKitConfig.getNetParams().equals(RegTestParams.get())) {
            this.connectToLocalHost();
        }

        this.setBlockingStartup(false);
        this.setAutoSave(true);
        this.setUserAgent("org.bytabit.mobile", AppConfig.getVersion());
    }

    String getFilePrefix() {
        return this.filePrefix;
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
