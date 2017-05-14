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
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.BackpressureOverflow;
import rx.Observable;
import rx.schedulers.JavaFxScheduler;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.*;

import static com.google.common.util.concurrent.Service.Listener;
import static org.bitcoinj.core.Transaction.DEFAULT_TX_FEE;

public abstract class WalletManager {

    private final static Logger LOG = LoggerFactory.getLogger(WalletManager.class);

    private final static NetworkParameters netParams;

    final Context btcContext;
    final WalletAppKit kit;

    final Observable<BlockDownloadEvent> blkDownloadEvents;
    final Observable<TransactionUpdatedEvent> txUpdatedEvents;

    final ObservableList<TransactionWithAmt> transactions;
    final StringProperty balance;

    private final DoubleProperty downloadProgress;
    private final BooleanProperty walletRunning;

    static {
        netParams = NetworkParameters.fromID("org.bitcoin." + AppConfig.getBtcNetwork());
    }

    WalletManager(String configName, WalletPurpose walletPurpose) {

        this.transactions = FXCollections.observableArrayList();
        this.balance = new SimpleStringProperty();
        this.downloadProgress = new SimpleDoubleProperty();
        this.walletRunning = new SimpleBooleanProperty(false);
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
                    txsWithAmt.add(new TransactionWithAmt(t, t.getValue(kit.wallet()),
                            getWatchedOutputAddress(t)));
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

                            TransactionWithAmt txu = new TransactionWithAmt(txe.getTx(),
                                    txe.getAmt(), getWatchedOutputAddress(txe.getTx()));

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

    public DoubleProperty downloadProgressProperty() {
        return downloadProgress;
    }

    public BooleanProperty walletRunningProperty() {
        return walletRunning;
    }

    void stopWallet() {
        Context.propagate(btcContext);
        kit.stopAsync();
    }

    public ObservableList<TransactionWithAmt> getTransactions() {
        return transactions;
    }

    public NetworkParameters getNetParams() {
        return netParams;
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

    public String fundEscrow(String escrowAddress, BigDecimal amount) throws InsufficientMoneyException {
        // TODO determine correct amount for extra tx fee for payout, current using DEFAULT_TX_FEE
        SendRequest sendRequest = SendRequest.to(Address.fromBase58(netParams, escrowAddress),
                Coin.parseCoin(amount.toString()).add(DEFAULT_TX_FEE));
        Transaction tx = kit.wallet().sendCoins(sendRequest).tx;
        return tx.getHashAsString();
    }

    public static String escrowAddress(String arbitratorProfilePubKey,
                                       String sellerEscrowPubKey,
                                       String buyerEscrowPubKey) {
        ECKey apk = ECKey.fromPublicOnly(Base58.decode(arbitratorProfilePubKey));
        ECKey spk = ECKey.fromPublicOnly(Base58.decode(sellerEscrowPubKey));
        ECKey bpk = ECKey.fromPublicOnly(Base58.decode(buyerEscrowPubKey));
        List<ECKey> pubkeys = Arrays.asList(apk, spk, bpk);

        return ScriptBuilder.createP2SHOutputScript(2, pubkeys).getToAddress(netParams).toBase58();
    }

    private ECKey getECKeyFromAddress(Address address) {
        return kit.wallet().findKeyFromPubHash(address.getHash160());
    }

    private String getWatchedOutputAddress(Transaction tx) {
        List<String> watchedOutputAddresses = new ArrayList<>();
        for (TransactionOutput output : tx.getOutputs()) {
            Address address = output.getAddressFromP2PKHScript(netParams);
            if (address != null && kit.wallet().isAddressWatched(address)) {
                watchedOutputAddresses.add(address.toBase58());
            } else {
                address = output.getAddressFromP2SH(netParams);
                if (address != null) {
                    watchedOutputAddresses.add(address.toBase58());
                } else {
                    // TODO throw error
                    LOG.error("Found output to unknown address type.");
                }
            }
        }

        if (watchedOutputAddresses.size() > 1) {
            LOG.error("Found more than one watched output address.");
        } else if (watchedOutputAddresses.size() < 1) {
            LOG.error("Did not find a watched output address");
        }
        return watchedOutputAddresses.size() > 0 ? watchedOutputAddresses.get(0) : null;
    }
}
