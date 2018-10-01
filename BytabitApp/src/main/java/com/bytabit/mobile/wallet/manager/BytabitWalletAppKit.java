package com.bytabit.mobile.wallet.manager;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.kits.WalletAppKit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

public class BytabitWalletAppKit extends WalletAppKit {

    public BytabitWalletAppKit(NetworkParameters params, File directory, String filePrefix) {
        super(params, directory, filePrefix);
    }

    protected void onSetupCompleted() {

        Path walletBackupFilePath = directory.toPath().resolve(String.format("%s.wallet.bkp", filePrefix));

        if (!Files.exists(walletBackupFilePath)) {
            try {
                Files.copy(vWalletFile.toPath(), walletBackupFilePath, COPY_ATTRIBUTES);
            } catch (IOException ioe) {
                log.error("Could not make backup copy of {} to {}", vWalletFile.toPath(), walletBackupFilePath);
            }
        }
    }
}
