package com.bytabit.ft.wallet;

import com.bytabit.ft.config.AppConfig;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;

public abstract class WalletManager {

    private final String configName;
    private final NetworkParameters netParams;
    private final Context btcContext;

    private final WalletAppKit kit;

//    private Observable<WalletEvent> walletDownloadEvents;
//    private Observable<WalletEvent> walletServiceEvents;

    WalletManager(String configName) {
        this.configName = configName;
        netParams = NetworkParameters.fromID("org.bitcoin." + AppConfig.getBtcNetwork());
        btcContext = Context.getOrCreate(netParams);
        kit = new WalletAppKit(btcContext, AppConfig.getPrivateStorage(), configName);

        // create observable download events
//        walletDownloadEvents = Observable.create((Observable.OnSubscribe<WalletEvent>) subscriber -> {
//            kit.setDownloadListener(new DownloadProgressTracker() {
//                @Override
//                public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {
//                    super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft);
//                    subscriber.onNext(new BlockDownloaded(peer, block, filteredBlock, blocksLeft));
//                }
//
//                @Override
//                protected void progress(double pct, int blocksSoFar, Date date) {
//                    super.progress(pct, blocksSoFar, date);
//                    subscriber.onNext(new DownloadProgress(pct, blocksSoFar, LocalDateTime.fromDateFields(date)));
//                }
//
//                @Override
//                protected void doneDownload() {
//                    super.doneDownload();
//                    subscriber.onNext(new DownloadDone());
//                }
//            });
//
//            subscriber.add(Subscriptions.create(() -> kit.setDownloadListener(null)));
//        }).subscribeOn(Schedulers.from(FiatTraderMobile.EXECUTOR)).share();
    }

    WalletAppKit startWallet() {
        Context.propagate(btcContext);

        // add service and download observables

        // setup wallet app kit
        kit.setAutoSave(true);
        kit.setBlockingStartup(false);
        kit.setUserAgent("org.bytabit.ft", AppConfig.getVersion());
        //kit.setDownloadListener(downloadProgressTracker);
        // TODO figure out which dispatcher to use
        //kit.addListener(kitListener, FiatTraderMobile.EXECUTOR);
        if (netParams.equals(RegTestParams.get())) {
            //kit.setPeerNodes(new PeerAddress(netParams, "regtest.bytabit.net", 18444));
            kit.connectToLocalHost();
        }
        // start wallet app kit
        kit.startAsync();
        return kit;
    }

    void stopWallet() {
        Context.propagate(btcContext);
        kit.stopAsync();
    }

//    public Observable<WalletEvent> getWalletDownloadEvents() {
//        return walletDownloadEvents;
//    }
}
