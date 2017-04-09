package com.bytabit.mobile.wallet;

import com.bytabit.mobile.BytabitMobile;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.wallet.evt.*;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import rx.schedulers.JavaFxScheduler;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.util.concurrent.Service.Listener;

public abstract class WalletManager {

    private static Logger LOG = LoggerFactory.getLogger(WalletManager.class);

    private final NetworkParameters netParams;
    private final Context btcContext;

    private final WalletAppKit kit;

    private final Observable<BlockDownloadEvent> blkDownloadEvents;
    private final Observable<TransactionUpdatedEvent> txUpdatedEvents;

    private final ObservableList<TransactionWithAmt> transactions;
    private final StringProperty balance;
    private final DoubleProperty downloadProgress;
    private final BooleanProperty walletRunning;

    WalletManager(String configName, WalletPurpose walletPurpose) {

        this.transactions = FXCollections.observableArrayList();
        this.balance = new SimpleStringProperty();
        this.downloadProgress = new SimpleDoubleProperty();
        this.walletRunning = new SimpleBooleanProperty(false);

        this.netParams = NetworkParameters.fromID("org.bitcoin." + AppConfig.getBtcNetwork());
        this.btcContext = Context.getOrCreate(netParams);
        Context.propagate(btcContext);

        kit = new WalletAppKit(btcContext, AppConfig.getPrivateStorage(), configName + walletPurpose);

        // create observable download events
        blkDownloadEvents = Observable.create((Observable.OnSubscribe<BlockDownloadEvent>) subscriber -> {
            kit.setDownloadListener(new DownloadProgressTracker() {
                @Override
                public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {
                    super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft);
                    subscriber.onNext(new BlockDownloaded(peer, block, filteredBlock, blocksLeft));
                }

                @Override
                protected void progress(double pct, int blocksSoFar, Date date) {
                    super.progress(pct, blocksSoFar, date);
                    subscriber.onNext(new BlockDownloadProgress(pct / 100.00, blocksSoFar, LocalDateTime.fromDateFields(date)));
                }

                @Override
                protected void doneDownload() {
                    super.doneDownload();
                    subscriber.onNext(new BlockDownloadDone());
                }
            });

            subscriber.add(Subscriptions.create(() ->
                    kit.setDownloadListener(null)));
        }).onBackpressureBuffer(100, null, BackpressureOverflow.ON_OVERFLOW_DROP_OLDEST).share();

        // create observable wallet running events
        txUpdatedEvents = Observable.create((Observable.OnSubscribe<TransactionUpdatedEvent>) subscriber -> {
            TransactionConfidenceEventListener listener = new TransactionConfidenceEventListener() {

                @Override
                public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                    Context.propagate(btcContext);
                    subscriber.onNext(new TransactionUpdatedEvent(tx, wallet));
                }
            };
            kit.wallet().addTransactionConfidenceEventListener(BytabitMobile.EXECUTOR, listener);

            subscriber.add(Subscriptions.create(() -> kit.wallet().removeTransactionConfidenceEventListener(listener)));
        }).onBackpressureBuffer(100, null, BackpressureOverflow.ON_OVERFLOW_DROP_OLDEST).share();

        // add TX observer when wallet is running
        kit.addListener(new Listener() {

            @Override
            public void running() {
                walletRunning.setValue(true);

                // get existing tx
                Set<TransactionWithAmt> txsWithAmt = new HashSet<>();
                for (Transaction t : kit.wallet().getTransactions(false)) {
                    txsWithAmt.add(new TransactionWithAmt(t, t.getValue(kit.wallet())));
                }
                Platform.runLater(() -> {
                    transactions.addAll(txsWithAmt);
                    balance.setValue(kit.wallet().getBalance().toFriendlyString());
                });

                // listen for other events
                txUpdatedEvents.observeOn(JavaFxScheduler.getInstance())
                        .subscribe(e -> {
                            LOG.debug("tx updated event : {}", e);
                            TransactionUpdatedEvent txe = TransactionUpdatedEvent.class.cast(e);
                            TransactionWithAmt txu = new TransactionWithAmt(txe.getTx(), txe.getAmt());
                            Platform.runLater(() -> {
                                Integer index = transactions.indexOf(txu);
                                if (index > -1) {
                                    transactions.set(index, txu);
                                } else {
                                    transactions.add(txu);
                                }
                                balance.setValue(getWalletBalance().toFriendlyString());
                            });
                        });

                blkDownloadEvents.observeOn(JavaFxScheduler.getInstance())
                        .subscribe(e -> {
                            LOG.debug("block download event : {}", e);
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (e instanceof BlockDownloadDone) {
                                        BlockDownloadDone dde = BlockDownloadDone.class.cast(e);
                                        downloadProgress.setValue(1.0);
                                    } else if (e instanceof BlockDownloadProgress) {
                                        BlockDownloadProgress dpe = BlockDownloadProgress.class.cast(e);
                                        downloadProgress.setValue(dpe.getPct());
                                    }
                                    balance.setValue(getWalletBalance().toFriendlyString());
                                }
                            });
                        });

            }
        }, BytabitMobile.EXECUTOR);

        // setup wallet app kit
        kit.setAutoSave(true);
        kit.setBlockingStartup(false);
        kit.setUserAgent("org.bytabit.mobile", AppConfig.getVersion());

        if (netParams.equals(RegTestParams.get())) {
            //kit.setPeerNodes(new PeerAddress(netParams, "regtest.bytabit.net", 18444));
            kit.connectToLocalHost();
        }

        Context.propagate(btcContext);
        // start wallet app kit
        kit.startAsync();
    }

    public ObservableList<TransactionWithAmt> getTransactions() {
        return transactions;
    }

    public StringProperty balanceProperty() {
        return balance;
    }

    public DoubleProperty downloadProgressProperty() {
        return downloadProgress;
    }

    public BooleanProperty walletRunningProperty() {
        return walletRunning;
    }

//    public WalletAppKit startWallet() {
//        if (!walletRunning.getValue()) {
//
//            Context.propagate(btcContext);
//            // start wallet app kit
//            kit.startAsync();
//        }
//        return kit;
//    }

    void stopWallet() {
        Context.propagate(btcContext);
        kit.stopAsync();
    }

    public Address getDepositAddress() {
        return kit.wallet().currentReceiveAddress();
    }

    public Address getFreshAuthenticationAddress() {
        return kit.wallet().freshKey(KeyChain.KeyPurpose.AUTHENTICATION).toAddress(netParams);
    }

    public Address getFreshReceiveAddress() {
        return kit.wallet().freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).toAddress(netParams);
    }

    public String getFreshBase58PubKey() {
        return Base58.encode(kit.wallet().freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).getPubKey());
    }

    public Coin getWalletBalance() {
        return kit.wallet().getBalance();
    }

    private ECKey getECKeyFromAddress(Address address) {
        return kit.wallet().findKeyFromPubHash(address.getHash160());
    }
}
