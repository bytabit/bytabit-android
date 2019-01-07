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

import org.bitcoinj.wallet.UnreadableWalletException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class WalletManagerTest {

    private static final File TEMP_DIR = new File("./tmp");

    @Before
    public void before() {
        TEMP_DIR.mkdir();
    }

    @After
    public void after() {
        File[] tempFiles = TEMP_DIR.listFiles();
        if (tempFiles != null && tempFiles.length > 0) {
            Arrays.asList(tempFiles).forEach(f -> f.delete());
        }
        TEMP_DIR.delete();
    }

    @Test
    @Ignore
    public void testLoadWallet_whenNoWalletExist_createNewWalletAndBackup() throws IOException, UnreadableWalletException {

        //TestObserver<Wallet> testWalletObserver = new TestObserver<>();

        //File testWalletFile = new File("tmp/test.wallet");
        //File testWalletBackupFile = new File(testWalletFile.getPath() + WalletManager.BACKUP_EXT);

        //WalletManager walletManager = new WalletManager();

        //assertThat(testWalletFile.exists(), is(false));
        //assertThat(testWalletBackupFile.exists(), is(false));

        //Wallet wallet = walletManager.createOrLoadWallet(testWalletFile, testWalletBackupFile);

        //assertThat(wallet.isConsistent(), is(true));

        //assertThat(testWalletFile.exists(), is(true));
        //assertThat(testWalletFile.getPath(), is("tmp/test.wallet"));

        //assertThat(testWalletBackupFile.exists(), is(true));
        //assertThat(testWalletBackupFile.getPath(), is("tmp/test.wallet" + WalletManager.BACKUP_EXT));
    }
}
