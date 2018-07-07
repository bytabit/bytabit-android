package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.common.StorageManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.trade.model.BuyRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.fasterxml.jackson.jr.ob.JSON;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import org.bitcoinj.core.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

public class TradeManager {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    WalletManager walletManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    StorageManager storageManager;

//    @Inject
//    SellerProtocol sellerProtocol;
//
//    @Inject
//    BuyerProtocol buyerProtocol;
//
//    @Inject
//    ArbitratorProtocol arbitratorProtocol;

    private final TradeService tradeService;

    private final PublishSubject<Trade> createdTrade = PublishSubject.create();

    private final PublishSubject<Trade> updatedTrade = PublishSubject.create();

    private final PublishSubject<Trade> selectedTrade = PublishSubject.create();

    public final static String TRADES_PATH = AppConfig.getPrivateStorage().getPath() + File.separator +
            "trades" + File.separator;

    public TradeManager() {
        tradeService = new TradeService();
    }

    @PostConstruct
    public void initialize() {

//        Observable.zip(Observable.interval(15, TimeUnit.SECONDS, Schedulers.io()),
//                profileManager.loadOrCreateMyProfile(), (tick, profile) ->
//                        tradeService.get(profile.getPubKey())
//                                .retryWhen(error -> error.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS)))
//                                .flattenAsObservable(l -> l))
//                .flatMap(trades -> trades)
//                .map(this::handleTrade)
//                .observeOn(Schedulers.io())
//                .subscribeOn(Schedulers.io())
//                .subscribe(updatedTrade::onNext);

        // filter to allow only valid received trades
//                .filter(tr -> (tr.getCurrentTrade() == null && tr.getReceivedTrade().status().equals(CREATED))
//                        || (tr.getCurrentTrade().status().nextValid().contains(tr.getReceivedTrade().status())))
//                // TODO validate and merge trade states
//                // write received trade
//                .flatMap(tr -> writeTrade(tr.getReceivedTrade())
//                        .map(t -> new TradeReceived(tr.getCurrentTrade(), t))).share();

//        Observable<BuyerRequestedTrade> buyerRequestedTradeObservable = tradesReceivedObservable
//                .map(TradeReceived::getReceivedTrade)
//                .filter(t -> t.getRole().equals(Trade.Role.SELLER) && t.status().equals(CREATED))
//                .map(t -> new BuyerRequestedTrade(t.getEscrowAddress(), t.sellOffer(), t.buyRequest()));
//

//
//        Observable<TradeUpdated> tradeFundingUpdatesObservable = tradeWalletResults
//                .ofType(WalletManager.EscrowFunding.class)
//                .flatMap(funding -> readTrade(funding.getEscrowAddress())
//                        .map(t -> {
//                            PaymentDetails paymentDetails = profileManager.getPaymentDetails().autoConnect()
//                                    .filter(d -> d.getCurrencyCode().equals(t.sellOffer().getCurrencyCode()) && d.getPaymentMethod().equals(t.sellOffer().getPaymentMethod()))
//                                    .blockingLast();
//
//                            return Trade.builder()
//                                    .role(t.getRole())
//                                    .escrowAddress(t.getEscrowAddress())
//                                    .sellOffer(t.sellOffer())
//                                    .buyRequest(t.buyRequest())
//                                    .paymentRequest(new PaymentRequest(funding.getTransactionHash(),
//                                            paymentDetails.getPaymentDetails(), funding.getRefundAddress(),
//                                            funding.getRefundTxSignature()))
//                                    .build();
//                        }))
//                .map(TradeUpdated::new).share();

    }

    public void createBuyOffer(SellOffer sellOffer, BigDecimal btcAmount) {

        Observable.zip(walletManager.getTradeWalletEscrowPubKey().toObservable(),
                profileManager.loadOrCreateMyProfile().map(Profile::getPubKey).toObservable(),
                walletManager.getTradeWalletDepositAddress().map(Address::toBase58),
                (buyerEscrowPubKey, buyerProfilePubKey, buyerPayoutAddress) ->
                        new BuyRequest(buyerEscrowPubKey, btcAmount, buyerProfilePubKey, buyerPayoutAddress))
                .map(buyRequest -> Trade.builder()
                        .sellOffer(sellOffer)
                        .buyRequest(buyRequest)
                        .build())
                .flatMap(this::writeTrade)
                .doOnNext(t -> walletManager.createdEscrowWallet(t.getEscrowAddress()))
                .flatMap(t -> tradeService.put(t).toObservable())
                .subscribe(createdTrade::onNext);
    }

    //    public void initialize() {

//        if (tradeEvents == null) {

//            Observable<TradeEvent> readTrades = profileManager.loadOrCreateMyProfile().toObservable()
//                    .flatMap(profile -> Observable.create(source -> {
//                        // load stored tradeEvents
//                        File tradesDir = new File(TRADES_PATH);
//                        if (!tradesDir.exists()) {
//                            tradesDir.mkdirs();
//                        } else if (tradesDir.list() != null) {
//                            for (String tradeId : tradesDir.list()) {
//                                try {
//                                    File tradeFile = new File(TRADES_PATH + tradeId + File.separator + "currentTrade.json");
//                                    if (tradeFile.exists()) {
//                                        FileReader tradeReader = new FileReader(tradeFile);
//                                        Trade currentTrade = JSON.std.beanFrom(Trade.class, tradeReader);
//                                        emitTradeEvents(source, profile, currentTrade);
//                                    }
//                                } catch (IOException ioe) {
//                                    source.onError(ioe);
//                                }
//                            }
//                        }
//                        source.onComplete();
//                    }));
//
//            Observable<TradeEvent> receivedTrades = profileManager.loadOrCreateMyProfile().toObservable()
//                    .flatMap(profile -> Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
//                            .flatMap(tick -> tradeService.get(profile.getPubKey())
//                                    .retryWhen(errors ->
//                                            errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS))
//                                    ).flattenAsObservable(tl -> tl))
//                            .flatMap(currentTrade -> Observable.create(source -> emitTradeEvents(source, profile, currentTrade))));

//            createdTradeEvents = PublishSubject.create();

    //

    // post or patch all created currentTrade events


//            updatedTrades = tradeEvents.scan(new HashMap<String, Trade>(), (ts, te) -> {
//                Trade current = ts.get(te.getEscrowAddress());
//                if (current == null && te instanceof BuyerCreated) {
//                    BuyerCreated created = (BuyerCreated) te;
//                    Trade currentTrade = Trade.builder()
//                            .escrowAddress(te.getEscrowAddress())
//                            .sellOffer(created.getSellOffer())
//                            .buyRequest(created.getBuyRequest())
//                            .build();
//                    ts.put(created.getEscrowAddress(), currentTrade);
//                } else if (current != null && te instanceof Funded) {
//                    Funded funded = (Funded) te;
//                    Trade currentTrade = Trade.builder()
//                            .escrowAddress(te.getEscrowAddress())
//                            .sellOffer(current.sellOffer())
//                            .buyRequest(current.buyRequest())
//                            .paymentRequest(funded.getPaymentRequest())
//                            .fundingTransactionWithAmt(funded.getTransactionWithAmt())
//                            .build();
//                    ts.put(currentTrade.getEscrowAddress(), currentTrade);
//                } else if (current != null && te instanceof Paid) {
//                    Paid paid = (Paid) te;
//                    Trade currentTrade = Trade.builder()
//                            .escrowAddress(te.getEscrowAddress())
//                            .sellOffer(current.sellOffer())
//                            .buyRequest(current.buyRequest())
//                            .paymentRequest(current.paymentRequest())
//                            .fundingTransactionWithAmt(current.fundingTransactionWithAmt())
//                            .payoutRequest(paid.getPayoutRequest())
//                            .build();
//                    ts.put(currentTrade.getEscrowAddress(), currentTrade);
//                } else if (current != null && te instanceof Completed) {
//                    Completed completed = (Completed) te;
//                    Trade currentTrade = Trade.builder()
//                            .escrowAddress(te.getEscrowAddress())
//                            .sellOffer(current.sellOffer())
//                            .buyRequest(current.buyRequest())
//                            .paymentRequest(current.paymentRequest())
//                            .fundingTransactionWithAmt(current.fundingTransactionWithAmt())
//                            .payoutRequest(current.payoutRequest())
//                            .payoutCompleted(completed.getPayoutCompleted())
//                            .payoutTransactionWithAmt(completed.getPayoutTransactionWithAmt())
//                            .build();
//                    ts.put(currentTrade.getEscrowAddress(), currentTrade);
//                } else if (current != null && te instanceof Arbitrating) {
//                    Arbitrating arbitrating = (Arbitrating) te;
//                    Trade currentTrade = Trade.builder()
//                            .escrowAddress(te.getEscrowAddress())
//                            .sellOffer(current.sellOffer())
//                            .buyRequest(current.buyRequest())
//                            .paymentRequest(current.paymentRequest())
//                            .fundingTransactionWithAmt(current.fundingTransactionWithAmt())
//                            .payoutRequest(current.payoutRequest())
//                            .payoutCompleted(current.payoutCompleted())
//                            .payoutTransactionWithAmt(current.payoutTransactionWithAmt())
//                            .arbitrateRequest(arbitrating.getArbitrateRequest())
//                            .build();
//                    ts.put(currentTrade.getEscrowAddress(), currentTrade);
//                } else {
//                    //LOG.error("Unable to update Trade with TradeEvent.");
//                }
//
//                return ts;
//            }).flatMap(ts -> Observable.fromIterable(ts.values())).distinct()
//                    .replay().autoConnect(2);

    // write updated trades to local storage
//            updatedTrades.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
//                    .flatMap(currentTrade -> writeTrade(currentTrade).toObservable()).subscribe(currentTrade -> {
//                //LOG.debug(String.format("Writing updated currentTrade: %s", currentTrade.toString()));
//            });

//            tradeEvents = profileManager.loadOrCreateMyProfile().toObservable().subscribeOn(Schedulers.io())
//                    .flatMap(profile -> readTrades.map(currentTrade -> currentTrade.isLoaded(true))
//                            .concatWith(receivedTrades).mergeWith(createdTradeEvents)
//                            .scan(new HashMap<String, Trade>(), (currentTrades, foundTrade) -> {
//                                for (Trade currentTrade : currentTrades.values()) {
//                                    currentTrade.isUpdated(false);
//                                    currentTrade.isLoaded(false);
//                                }
//                                Trade currentTrade = currentTrades.get(foundTrade.getEscrowAddress());
//                                if (currentTrade == null) {
//                                    foundTrade.isUpdated(true);
//                                    currentTrades.put(foundTrade.getEscrowAddress(), foundTrade);
//                                } else {
//                                    Trade receivedTrade = handleTrade(currentTrade, profile, foundTrade);
//                                    if (receivedTrade != null) {
//                                        receivedTrade.isUpdated(true);
//                                        currentTrades.put(receivedTrade.getEscrowAddress(), receivedTrade);
//                                    }
//                                }
//                                return currentTrades;
//                            }))
//                    .flatMap(currentTrades -> Observable.fromIterable(currentTrades.values()))
//                    .filter(Trade::isUpdated).publish().autoConnect(2);

//            tradeEvents.observeOn(Schedulers.io()).subscribe(this::writeTrade);

//            Observable<List<Trade>> storedTrades = Single.<List<Trade>>create(source -> {
//                // load stored tradeEvents
//                List<Trade> trades = new ArrayList<>();
//                File tradesDir = new File(TRADES_PATH);
//                if (!tradesDir.exists()) {
//                    tradesDir.mkdirs();
//                }
//                if (tradesDir.list() != null) {
//                    for (String tradeId : tradesDir.list()) {
//                        try {
//                            File tradeFile = new File(TRADES_PATH + tradeId + File.separator + "currentTrade.json");
//                            if (tradeFile.exists()) {
//                                FileReader tradeReader = new FileReader(tradeFile);
//                                Trade currentTrade = JSON.std.beanFrom(Trade.class, tradeReader);
//                                trades.add(currentTrade);
//                            }
//                        } catch (IOException ioe) {
//                            source.onError(ioe);
//                        }
//                    }
//                } else {
//                    tradesDir.mkdirs();
//                }
//                source.onSuccess(trades);
//            }).toObservable();
//
//            Observable<List<Trade>> watchedTrades = profileManager.loadOrCreateMyProfile().toObservable()
//                    .flatMap(profile -> Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
//                            .flatMap(tick -> tradeService.get(profile.getPubKey()).retry().toObservable()));
//
//            createdTradeEvents = PublishSubject.create();
//
//            tradeEvents = profileManager.loadOrCreateMyProfile().toObservable().subscribeOn(Schedulers.io())
//                    .flatMap(profile -> storedTrades.concatWith(watchedTrades).mergeWith(createdTradeEvents)
//                            .scan(new HashMap<String, Trade>(), (currentTrades, foundTrades) -> {
//                                for (Trade currentTrade : currentTrades.values()) {
//                                    currentTrade.isUpdated(false);
//                                }
//                                for (Trade foundTrade : foundTrades) {
//                                    Trade receivedTrade = handleTrade(currentTrades, profile, foundTrade);
//                                    if (receivedTrade != null) {
//                                        receivedTrade.isUpdated(true);
//                                        currentTrades.put(receivedTrade.getEscrowAddress(), receivedTrade);
//                                    }
//                                }
//                                return currentTrades;
//                                //}).map(currentTrades -> (List<Trade>)new ArrayList<>(currentTrades.values()))).publish();
//                            }))
//                    .flatMap(currentTrades -> Observable.fromIterable(currentTrades.values()))
//                    .filter(Trade::isUpdated).publish().autoConnect();
//
//            tradeEvents.observeOn(Schedulers.io()).subscribe(this::writeTrade);
//    }
//    }

//    public BuyerCreated buyerCreateTrade(SellOffer sellOffer, BuyRequest buyRequest) {
//
//        String escrowAddress = walletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(),
//                sellOffer.getSellerEscrowPubKey(), buyRequest.getBuyerEscrowPubKey());
//        BuyerCreated created = new BuyerCreated(escrowAddress, BUYER, sellOffer, buyRequest);
//        createdTradeEvents.onNext(created);
//        return created;
//    }

//    public Single<Trade> buyerCreateTrade(SellOffer sellOffer, BigDecimal buyBtcAmount,
//                                          String buyerEscrowPubKey, String buyerProfilePubKey,
//                                          String buyerPayoutAddress) {
//
//        return buyerProtocol.buyerCreateTrade(sellOffer, buyBtcAmount, buyerEscrowPubKey, buyerProfilePubKey, buyerPayoutAddress);
//    }

//    public void buyerSendPayment(String paymentReference) {
//        buyerProtocol.sendPayment(selectedTrade.getValue(), paymentReference);
//    }
//
//    public void sellerConfirmPaymentReceived() {
//        sellerProtocol.confirmPaymentReceived(selectedTrade.getValue());
//    }

//    public void requestArbitrate() {

//        profileManager.loadOrCreateMyProfile().observeOn(JavaFxScheduler.platform()).subscribe(profile -> {
//            Trade currentTrade = selectedTrade.getValue();
//
//            String profilePubKey = profile.getPubKey();
//            Boolean profileIsArbitrator = profile.isArbitrator();
//
//            Trade.Role role = currentTrade.role(profilePubKey, profileIsArbitrator);
//            if (role.equals(SELLER)) {
//                sellerProtocol.requestArbitrate(currentTrade);
//            } else if (role.equals(BUYER)) {
//                buyerProtocol.requestArbitrate(currentTrade);
//            }
//        });
//    }

//    public void arbitratorRefundSeller() {
//        Trade currentTrade = selectedTrade.getValue();
//        arbitratorProtocol.refundSeller(currentTrade);
//    }
//
//    public void arbitratorPayoutBuyer() {
//        Trade currentTrade = selectedTrade.getValue();
//        arbitratorProtocol.payoutBuyer(currentTrade);
//    }
//
//    public void buyerCancel() {
//        Trade currentTrade = selectedTrade.getValue();
//        buyerProtocol.cancelTrade(currentTrade);
//    }
//
//    public void setSelectedTrade(Trade currentTrade) {
//        Platform.runLater(() -> {
//            selectedTrade.setValue(currentTrade);
//        });
//    }

    //    public Single<List<Trade>> singleTrades(String profilePubKey) {
//        return tradeService.get(profilePubKey).retry().subscribeOn(Schedulers.io());
//    }
//
//    private Trade handleTrade(Trade currentTrade, Profile profile, Trade foundTrade) {
//
////        Trade currentTrade = currentTrades.get(foundTrade.getEscrowAddress());
//        Trade receivedTrade = null;
//
//        if (currentTrade == null || !currentTrade.status().equals(foundTrade.status())) {
//
//            String profilePubKey = profile.getPubKey();
//            Boolean profileIsArbitrator = profile.isArbitrator();
//
//            Trade.Role role = foundTrade.role(profilePubKey, profileIsArbitrator);
//            TradeProtocol tradeProtocol;
//
//            if (role.equals(SELLER)) {
//                tradeProtocol = sellerProtocol;
//            } else if (role.equals(BUYER)) {
//                tradeProtocol = buyerProtocol;
//            } else if (role.equals(ARBITRATOR)) {
//                tradeProtocol = arbitratorProtocol;
//            } else {
//                throw new RuntimeException("Unable to determine currentTrade protocol.");
//            }
//
//            switch (foundTrade.status()) {
//
//                case CREATED:
//                    //receivedTrade = currentTrade == null ? tradeProtocol.handleCreated(foundTrade) : null;
//                    break;
//
//                case FUNDED:
//                    receivedTrade = currentTrade != null ? tradeProtocol.handleFunded(currentTrade, foundTrade) : foundTrade;
//                    break;
//
//                case PAID:
//                    receivedTrade = currentTrade != null ? tradeProtocol.handlePaid(currentTrade, foundTrade) : foundTrade;
//                    break;
//
//                case COMPLETED:
//                    receivedTrade = currentTrade != null ? tradeProtocol.handleCompleted(currentTrade, foundTrade) : foundTrade;
//                    break;
//
//                case ARBITRATING:
//                    receivedTrade = currentTrade != null ? tradeProtocol.handleArbitrating(currentTrade, foundTrade) : foundTrade;
//                    break;
//            }
//        }
//
//        return receivedTrade;
//    }

    private Observable<Trade> writeTrade(Trade trade) {

        return Observable.create(source -> {
            String tradePath = TRADES_PATH + trade.getEscrowAddress() + File.separator + "currentTrade.json";

            try {
                File dir = new File(TRADES_PATH + trade.getEscrowAddress());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileWriter tradeWriter = new FileWriter(tradePath);
                tradeWriter.write(JSON.std.asString(trade));
                tradeWriter.flush();

                //LOG.debug("Wrote local currentTrade: {}", currentTrade);

            } catch (IOException ioe) {
                //LOG.error(ioe.getMessage());
                source.onError(ioe);
            }
            source.onNext(trade);
        });
    }

    private Optional<Trade> readTrade(String escrowAddress) throws IOException {
        File tradeFile = new File(TRADES_PATH + escrowAddress + File.separator + "currentTrade.json");
        if (tradeFile.exists()) {
            FileReader tradeReader = new FileReader(tradeFile);
            return Optional.of(JSON.std.beanFrom(Trade.class, tradeReader));
        } else {
            return Optional.empty();
        }
    }

//    private Observable<Trade> readTrades() {
//
//        return Observable.just(new File(TRADES_PATH))
//                .map(tradesDir -> {
//                    if (!tradesDir.exists()) {
//                        tradesDir.mkdirs();
//                    }
//                    return tradesDir;
//                })
//                .flatMapIterable(tradesDir -> {
//                    if (tradesDir.list() != null) {
//                        return Arrays.asList(tradesDir.list());
//                    } else {
//                        return new ArrayList<>();
//                    }
//                })
//                .flatMap(this::readTrade);
//    }

//        return Observable.create(source -> {
//
//                    // load stored trades
//                    File tradesDir = new File(TRADES_PATH);
//                    if (!tradesDir.exists()) {
//                        tradesDir.mkdirs();
//                    } else if (tradesDir.list() != null) {
//                        for (String tradeId : tradesDir.list()) {
//                            try {
//                                File tradeFile = new File(TRADES_PATH + tradeId + File.separator + "currentTrade.json");
//                                if (tradeFile.exists()) {
//                                    FileReader tradeReader = new FileReader(tradeFile);
//                                    Trade trade = JSON.std.beanFrom(Trade.class, tradeReader);
//                                    source.onNext(trade);
//                                }
//                            } catch (IOException ioe) {
//                                source.onError(ioe);
//                            }
//                        }
//                    }
//                    source.onComplete();
//                }
//        );

//    private Observable<TradeReceived> receiveTrade(Profile profile, Trade receivedTrade) {
//
//        return Observable.create(source -> {
//                    receivedTrade.role(profile.getPubKey(), profile.isArbitrator());
//                    String escrowAddress = receivedTrade.getEscrowAddress();
//                    if (escrowAddress != null) {
//                        try {
//                            File tradeFile = new File(TRADES_PATH + escrowAddress + File.separator + "currentTrade.json");
//                            if (tradeFile.exists()) {
//                                FileReader tradeReader = new FileReader(tradeFile);
//                                Trade currentTrade = JSON.std.beanFrom(Trade.class, tradeReader);
//                                source.onNext(new TradeReceived(currentTrade, receivedTrade));
//                            } else if (receivedTrade.status().equals(CREATED)) {
//                                source.onNext(new TradeReceived(null, receivedTrade));
//                            }
//                        } catch (IOException ioe) {
//                            source.onError(ioe);
//                        }
//                    }
//                    source.onComplete();
//                }
//        );
//    }

// convert currentTrade into stream of currentTrade events
//    private void emitTradeEvents(ObservableEmitter<TradeEvent> source, Profile profile, Trade currentTrade) {
//
//        String escrowAddress = currentTrade.getEscrowAddress();
//        Trade.Role role = currentTrade.role(profile.getPubKey(), profile.isArbitrator());
//
//        source.onNext(new com.bytabit.mobile.currentTrade.evt.BuyerCreated(escrowAddress, role, currentTrade.sellOffer(), currentTrade.buyRequest()));
//        if (currentTrade.status().compareTo(Trade.Status.FUNDING) >= 0) {
//            source.onNext(new Funded(escrowAddress, role, currentTrade.paymentRequest(), currentTrade.fundingTransactionWithAmt()));
//        }
//        if (currentTrade.status().compareTo(Trade.Status.PAID) >= 0) {
//            source.onNext(new Paid(escrowAddress, role, currentTrade.payoutRequest()));
//        }
//        if (currentTrade.status().compareTo(Trade.Status.ARBITRATING) == 0) {
//            source.onNext(new Arbitrating(escrowAddress, role, currentTrade.arbitrateRequest()));
//        }
//        if (currentTrade.status().compareTo(Trade.Status.COMPLETING) >= 0) {
//            source.onNext(new Completed(escrowAddress, role, currentTrade.payoutCompleted(), currentTrade.payoutTransactionWithAmt()));
//        }
//    }

}

//                                // only use valid next status
//                                if (currentTrade.status().nextValid().contains(receivedTrade.status())) {
//
//                                    // only use status from updated trade based on role
//                                    if (currentTrade.getRole().equals(SELLER)) {
//                                        switch (receivedTrade.status()) {
//                                            case FUNDING:
//                                                break;
//                                            case FUNDED:
//                                                break;
//                                            case PAID:
//                                                break;
//                                            case COMPLETING:
//                                                break;
//                                            case COMPLETED:
//                                                break;
//                                            case ARBITRATING:
//                                                break;
//                                        }
//                                    } else if (currentTrade.getRole().equals(BUYER)) {
//                                        switch (receivedTrade.status()) {
//                                            case FUNDING:
//                                                break;
//                                            case FUNDED:
//                                                break;
//                                            case PAID:
//                                                break;
//                                            case COMPLETING:
//                                                break;
//                                            case COMPLETED:
//                                                break;
//                                            case ARBITRATING:
//                                                break;
//                                        }
//                                    } else if (currentTrade.getRole().equals(ARBITRATOR)) {
//
//                                    } else {
//                                        // ERROR
//                                    }
//
//                                    Trade receivedTrade = new Trade();