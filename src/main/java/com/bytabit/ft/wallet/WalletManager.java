package com.bytabit.ft.wallet;

import com.bytabit.ft.config.AppConfig;
import com.google.common.util.concurrent.Service.Listener;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class WalletManager {

    private final NetworkParameters netParams;
    private final Context btcContext;

    private WalletAppKit kit;
    private TransactionConfidence.Listener kitListener;

    private final Executor executor;

    WalletManager() {
        netParams = NetworkParameters.fromID(AppConfig.getBtcNetwork());
        btcContext = Context.getOrCreate(netParams);
        new WalletAppKit(btcContext, AppConfig.getPrivateStorage(), AppConfig.getConfigName());
        executor = Executors.newSingleThreadExecutor();
    }

    WalletAppKit startWallet(Listener kitListener, DownloadProgressTracker downloadProgressTracker) {
        Context.propagate(btcContext);
        // setup wallet app kit
        kit.setAutoSave(true);
        kit.setBlockingStartup(false);
        kit.setUserAgent("org.bytabit.ft", AppConfig.getVersion());
        kit.setDownloadListener(downloadProgressTracker);
        // TODO figure out which dispatcher to use
        kit.addListener(kitListener, executor);
        if (netParams.equals(RegTestParams.get())) {
            kit.setPeerNodes(new PeerAddress(netParams, "regtest.bytabit.org", 18444));
            //kit.connectToLocalHost()
        }
        // start wallet app kit
        kit.startAsync();
        return kit;
    }

    void stopWallet(WalletAppKit kit) {
        Context.propagate(btcContext);
        kit.stopAsync();
    }
}
