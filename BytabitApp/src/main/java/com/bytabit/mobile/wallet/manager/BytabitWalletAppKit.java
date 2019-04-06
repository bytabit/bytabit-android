/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.common.file.FileUtils;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.wallet.model.WalletKitConfig;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.wallet.DeterministicSeed;

import java.io.File;
import java.io.IOException;

public class BytabitWalletAppKit extends WalletAppKit {

    private final WalletKitConfig walletKitConfig;

    BytabitWalletAppKit(WalletKitConfig walletKitConfig, DeterministicSeed currentSeed) {
        super(walletKitConfig.getNetParams(), walletKitConfig.getDirectory(), walletKitConfig.getFilePrefix());
        this.walletKitConfig = walletKitConfig;

        // determine reset wallet creation time
        long resetCreationTimeSeconds;
        if (walletKitConfig.getCreationTimeSeconds() != null) {
            resetCreationTimeSeconds = walletKitConfig.getCreationTimeSeconds();
        } else if (currentSeed != null) {
            resetCreationTimeSeconds = currentSeed.getCreationTimeSeconds();
        } else {
            resetCreationTimeSeconds = 0L;
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

        long ONE_MONTH_MILLISECONDS = 60000 * 60 * 24 * 30;

        WalletKitConfig config = getWalletKitConfig();
        if (config.getWatchAddresses() != null && !config.getWatchAddresses().isEmpty()) {
            wallet().addWatchedAddresses(config.getWatchAddresses(), System.currentTimeMillis() - ONE_MONTH_MILLISECONDS * 2);
        }

        File walletBackupFile = new File(String.format("%s/%s.wallet.bkp", directory.getPath(), filePrefix));

        if (!walletBackupFile.exists()) {
            try {
                FileUtils.copy(vWalletFile, walletBackupFile);
            } catch (IOException ioe) {
                log.error("Could not make backup copy of {} to {}", vWalletFile.getPath(), walletBackupFile.getPath());
            }
        }
    }

    WalletKitConfig getWalletKitConfig() {
        return walletKitConfig;
    }
}
