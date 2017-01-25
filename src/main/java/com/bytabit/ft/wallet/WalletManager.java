package com.bytabit.ft.wallet;

import com.bytabit.ft.EventObservables;
import com.bytabit.ft.FiatTraderMobile;
import com.bytabit.ft.config.AppConfig;
import com.bytabit.ft.wallet.evt.*;
import com.google.common.util.concurrent.Service;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.joda.time.LocalDateTime;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nullable;
import java.util.Date;

public abstract class WalletManager {

    private final String configName;
    private final WalletPurpose walletPurpose;
    private final NetworkParameters netParams;
    private final Context btcContext;

    private final WalletAppKit kit;

    private Observable<WalletEvent> walletDownloadEvents;
    private Observable<WalletEvent> walletServiceEvents;

    WalletManager(String configName, WalletPurpose walletPurpose) {

        this.configName = configName;
        this.walletPurpose = walletPurpose;
        this.netParams = NetworkParameters.fromID("org.bitcoin." + AppConfig.getBtcNetwork());
        this.btcContext = Context.getOrCreate(netParams);
        Context.propagate(btcContext);

        kit = new WalletAppKit(btcContext, AppConfig.getPrivateStorage(), configName);

        // create observable download events
        walletDownloadEvents = Observable.create((Observable.OnSubscribe<WalletEvent>) subscriber -> {
            kit.setDownloadListener(new DownloadProgressTracker() {
                @Override
                public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {
                    super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft);
                    subscriber.onNext(new BlockDownloaded(peer, block, filteredBlock, blocksLeft));
                }

                @Override
                protected void progress(double pct, int blocksSoFar, Date date) {
                    super.progress(pct, blocksSoFar, date);
                    subscriber.onNext(new DownloadProgress(pct, blocksSoFar, LocalDateTime.fromDateFields(date)));
                }

                @Override
                protected void doneDownload() {
                    super.doneDownload();
                    subscriber.onNext(new DownloadDone());
                }
            });

            subscriber.add(Subscriptions.create(() -> kit.setDownloadListener(null)));
        }).subscribeOn(Schedulers.from(FiatTraderMobile.EXECUTOR)).share();

        // create observable wallet running events
        walletServiceEvents = Observable.create((Observable.OnSubscribe<WalletEvent>) subscriber -> {
            Service.Listener listener = new Service.Listener() {

                @Override
                public void running() {
                    subscriber.onNext(new WalletRunning(walletPurpose));
                }
            };
            kit.addListener(listener, FiatTraderMobile.EXECUTOR);
        }).subscribeOn(Schedulers.from(FiatTraderMobile.EXECUTOR)).share();

        // add observables to shared composite observable
        EventObservables.getWalletEvents().addAll(walletDownloadEvents, walletServiceEvents);

        // setup wallet app kit
        kit.setAutoSave(true);
        kit.setBlockingStartup(false);
        kit.setUserAgent("org.bytabit.ft", AppConfig.getVersion());

        if (netParams.equals(RegTestParams.get())) {
            //kit.setPeerNodes(new PeerAddress(netParams, "regtest.bytabit.net", 18444));
            kit.connectToLocalHost();
        }
    }

    WalletAppKit startWallet() {
        Context.propagate(btcContext);

        // start wallet app kit
        kit.startAsync();
        return kit;
    }

    void stopWallet() {
        Context.propagate(btcContext);
        kit.stopAsync();
    }

    public Observable<WalletEvent> getWalletDownloadEvents() {
        return walletDownloadEvents;
    }
}
