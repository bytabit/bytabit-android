package com.bytabit.mobile.wallet.manager;

import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
    public void testLoadWallet_whenNoWalletExist_createNewWalletAndBackup() throws IOException, UnreadableWalletException {

        //TestObserver<Wallet> testWalletObserver = new TestObserver<>();

        File testWalletFile = new File("tmp/test.wallet");
        File testWalletBackupFile = new File(testWalletFile.getPath() + WalletManager.BACKUP_EXT);

        WalletManager walletManager = new WalletManager();

        assertThat(testWalletFile.exists(), is(false));
        assertThat(testWalletBackupFile.exists(), is(false));

        Wallet wallet = walletManager.createOrLoadWallet(testWalletFile, testWalletBackupFile);

        assertThat(wallet.isConsistent(), is(true));

        assertThat(testWalletFile.exists(), is(true));
        assertThat(testWalletFile.getPath(), is("tmp/test.wallet"));

        assertThat(testWalletBackupFile.exists(), is(true));
        assertThat(testWalletBackupFile.getPath(), is("tmp/test.wallet" + WalletManager.BACKUP_EXT));
    }
}
