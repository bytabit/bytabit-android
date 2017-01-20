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

    private final String configName;
    private final NetworkParameters netParams;
    private final Context btcContext;

    private final WalletAppKit kit;
    private final Executor executor;
    private TransactionConfidence.Listener kitListener;

    WalletManager(String configName) {
        this.configName = configName;
        netParams = NetworkParameters.fromID("org.bitcoin."+AppConfig.getBtcNetwork());
        btcContext = Context.getOrCreate(netParams);
        executor = Executors.newSingleThreadExecutor();
        kit = new WalletAppKit(btcContext, AppConfig.getPrivateStorage(), configName);
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
            kit.setPeerNodes(new PeerAddress(netParams, "regtest.bytabit.net", 18444));
            //kit.connectToLocalHost()
        }
        // start wallet app kit
        kit.startAsync();
        return kit;
    }

    void stopWallet() {
        Context.propagate(btcContext);
        kit.stopAsync();
    }
}
