package com.bytabit.ft.wallet;

import com.bytabit.ft.FiatTraderMobile;
import com.bytabit.ft.config.AppConfig;
import com.bytabit.ft.wallet.evt.*;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.Wallet;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.BackpressureOverflow;
import rx.Observable;
import rx.javafx.sources.CompositeObservable;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nullable;
import java.util.Date;

import static com.google.common.util.concurrent.Service.Listener;

public abstract class WalletManager {

    private static Logger LOG = LoggerFactory.getLogger(WalletManager.class);

    private final NetworkParameters netParams;
    private final Context btcContext;

    private final WalletAppKit kit;

    private final CompositeObservable<WalletEvent> walletEvents;
    private final Observable<WalletEvent> walletDownloadEvents;
    private final Observable<WalletEvent> walletTxConfidenceEvents;

    WalletManager(String configName, WalletPurpose walletPurpose) {

        this.netParams = NetworkParameters.fromID("org.bitcoin." + AppConfig.getBtcNetwork());
        this.btcContext = Context.getOrCreate(netParams);
        Context.propagate(btcContext);

        kit = new WalletAppKit(btcContext, AppConfig.getPrivateStorage(), configName);

        // create observable composite wallet events
        walletEvents = new CompositeObservable<>();

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
                    subscriber.onNext(new DownloadProgress(pct/100.00, blocksSoFar, LocalDateTime.fromDateFields(date)));
                }

                @Override
                protected void doneDownload() {
                    super.doneDownload();
                    subscriber.onNext(new DownloadDone());
                }
            });

            subscriber.add(Subscriptions.create(() ->
                    kit.setDownloadListener(null)));
        }).onBackpressureLatest().share();

        // create observable wallet running events
        walletTxConfidenceEvents = Observable.create((Observable.OnSubscribe<WalletEvent>) subscriber -> {
            TransactionConfidenceEventListener listener = new TransactionConfidenceEventListener() {

                @Override
                public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                    Context.propagate(btcContext);
                    subscriber.onNext(new TransactionUpdatedEvent(tx, tx.getValue(wallet),
                            tx.getConfidence().getConfidenceType(),
                            tx.getConfidence().getDepthInBlocks()));
                }
            };
            kit.wallet().addTransactionConfidenceEventListener(FiatTraderMobile.EXECUTOR, listener);

            subscriber.add(Subscriptions.create(() -> kit.wallet().removeTransactionConfidenceEventListener(listener)));
        }).onBackpressureLatest().share();
        walletEvents.add(walletDownloadEvents);

        // add TX observer when wallet is running
        kit.addListener(new Listener() {

            @Override
            public void running() {
                // add observables to shared composite observable
                //walletEvents.add(walletDownloadEvents);
                walletEvents.add(walletTxConfidenceEvents);
            }
        }, FiatTraderMobile.EXECUTOR);

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

    public Observable<WalletEvent> getWalletEvents() {
        return walletEvents.toObservable()
                .onBackpressureBuffer(100, null, BackpressureOverflow.ON_OVERFLOW_DROP_OLDEST);
    }

    public Address getDepositAddress() {
        return kit.wallet().currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);
    }

    public Coin getWalletBalance() {
        return kit.wallet().getBalance();
    }
}
