package com.bytabit.mobile.trade;

import com.bytabit.mobile.common.*;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.trade.model.BuyRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.fasterxml.jackson.jr.ob.JSON;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.bytabit.mobile.trade.model.Trade.Status.CREATED;

public class TradeManager extends AbstractManager {

    private final EventLogger eventLogger = EventLogger.of(TradeManager.class);

//    @Inject
//    BuyerProtocol buyerProtocol;
//
//    @Inject
//    SellerProtocol sellerProtocol;
//
//    @Inject
//    ArbitratorProtocol arbitratorProtocol;

    private final TradeService tradeService;

    private final PublishSubject<TradeAction> actions;

    private final Observable<TradeResult> results;

//    @Inject
//    ProfileManager profileManager;

//    @Inject
//    WalletManager walletManager;

//    private Observable<TradeEvent> tradeEvents;
//
//    private PublishSubject<TradeEvent> createdTradeEvents;

    //private final Observable<Trade> trades;

//    private final ObjectProperty<Trade> selectedTrade = new SimpleObjectProperty<>();

    public final static String TRADES_PATH = AppConfig.getPrivateStorage().getPath() + File.separator +
            "trades" + File.separator;

    @Inject
    public TradeManager() {

        tradeService = new TradeService();

        actions = PublishSubject.create();

        Observable<TradeAction> actionObservable = actions
                .compose(eventLogger.logEvents())
                .startWith(new LoadTrades())
                .share();

        Observable<TradeReceived> tradesReceived = actionObservable.ofType(GetTrades.class)
                .map(GetTrades::getProfile)
                .flatMap(profile -> Observable.interval(30, TimeUnit.SECONDS, Schedulers.io())
                        .flatMap(tick -> tradeService.get(profile.getPubKey())
                                .retryWhen(errors -> errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS)))
                                .flattenAsObservable(l -> l))
                        .flatMap(trade -> receiveTrade(profile, trade)))
                // filter to allow only valid received trades
                .filter(tr -> (tr.getCurrentTrade() == null && tr.getReceivedTrade().status().equals(CREATED))
                        || (tr.getCurrentTrade().status().nextValid().contains(tr.getReceivedTrade().status())))
                // TODO validate and merge trade states
                // write received trade
                .flatMap(tr -> writeTrade(tr.getReceivedTrade())
                        .map(t -> new TradeReceived(tr.getCurrentTrade(), t)));

        Observable<TradeCreated> tradeCreatedObservable = actionObservable.ofType(CreateTrade.class)
                .map(ct -> new TradeCreated(Trade.builder()
                        .escrowAddress(ct.getEscrowAddress())
                        .role(ct.getRole())
                        .sellOffer(ct.getSellOffer())
                        .buyRequest(ct.getBuyRequest())
                        .build())).share();

        Observable<TradeWritten> tradeSavedObservable = tradeCreatedObservable
                .flatMap(tc -> writeTrade(tc.trade)
                        .map(TradeWritten::new));

        Observable<TradePut> tradePutObservable = tradeCreatedObservable
                .flatMap(tc -> tradeService.put(tc.trade)
                        .map(TradePut::new)
                        .toObservable());

        Observable<TradeLoaded> tradeLoadedObservable = actionObservable.ofType(LoadTrades.class)
                .flatMap(lt -> readTrades())
                .map(TradeLoaded::new);

        Observable<TradeSelected> tradeSelectedObservable = actionObservable.ofType(SelectTrade.class)
                .map(SelectTrade::getTrade)
                .map(TradeSelected::new);

        results = Observable.merge(tradeLoadedObservable, tradeCreatedObservable,
                tradeSavedObservable, tradePutObservable)
                .mergeWith(tradeSelectedObservable)
                .mergeWith(tradesReceived)
                .compose(eventLogger.logResults())
                .share();
    }

    public PublishSubject<TradeAction> getActions() {
        return actions;
    }

    public Observable<TradeResult> getResults() {
        return results;
    }

    //    public void initialize() {

//        if (tradeEvents == null) {

//            Observable<TradeEvent> readTrades = profileManager.loadMyProfile().toObservable()
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
//            Observable<TradeEvent> receivedTrades = profileManager.loadMyProfile().toObservable()
//                    .flatMap(profile -> Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
//                            .flatMap(tick -> tradeService.get(profile.getPubKey())
//                                    .retryWhen(errors ->
//                                            errors.flatMap(e -> Flowable.timer(100, TimeUnit.SECONDS))
//                                    ).flattenAsObservable(tl -> tl))
//                            .flatMap(currentTrade -> Observable.create(source -> emitTradeEvents(source, profile, currentTrade))));

//            createdTradeEvents = PublishSubject.create();

    //

    // post or patch all created currentTrade events


//            tradeEvents = readTrades.concatWith(receivedTrades).mergeWith(createdTradeEvents)
//                    .distinct().subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
//                    .replay().autoConnect(3);
//
//            tradeEvents.filter(te -> te.getRole().equals(BUYER)).subscribe(te -> {
//                // buyer protocol
//                if (te instanceof BuyerCreated) {
//                    buyerProtocol.handleCreated((BuyerCreated) te);
//                    LOG.debug("Created currentTrade event: {}", te);
//                }
//            });

//            tradeEvents.filter(te -> te.getRole().equals(SELLER)).subscribe(te -> {
//                // seller protocol
//            });
//
//            tradeEvents.filter(te -> te.getRole().equals(ARBITRATOR)).subscribe(te -> {
//                // arbitrator protocol
//            });

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

//            tradeEvents = profileManager.loadMyProfile().toObservable().subscribeOn(Schedulers.io())
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
//            Observable<List<Trade>> watchedTrades = profileManager.loadMyProfile().toObservable()
//                    .flatMap(profile -> Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
//                            .flatMap(tick -> tradeService.get(profile.getPubKey()).retry().toObservable()));
//
//            createdTradeEvents = PublishSubject.create();
//
//            tradeEvents = profileManager.loadMyProfile().toObservable().subscribeOn(Schedulers.io())
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

//        profileManager.loadMyProfile().observeOn(JavaFxScheduler.platform()).subscribe(profile -> {
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

    private Observable<Trade> readTrades() {

        return Observable.create(source -> {

            // load stored trades
                    File tradesDir = new File(TRADES_PATH);
                    if (!tradesDir.exists()) {
                        tradesDir.mkdirs();
                    } else if (tradesDir.list() != null) {
                        for (String tradeId : tradesDir.list()) {
                            try {
                                File tradeFile = new File(TRADES_PATH + tradeId + File.separator + "currentTrade.json");
                                if (tradeFile.exists()) {
                                    FileReader tradeReader = new FileReader(tradeFile);
                                    Trade trade = JSON.std.beanFrom(Trade.class, tradeReader);
                                    source.onNext(trade);
                                }
                            } catch (IOException ioe) {
                                source.onError(ioe);
                            }
                        }
                    }
                    source.onComplete();
                }
        );
    }

    private Observable<TradeReceived> receiveTrade(Profile profile, Trade receivedTrade) {

        return Observable.create(source -> {
            receivedTrade.role(profile.getPubKey(), profile.isArbitrator());
            String escrowAddress = receivedTrade.getEscrowAddress();
                    if (escrowAddress != null) {
                        try {
                            File tradeFile = new File(TRADES_PATH + escrowAddress + File.separator + "currentTrade.json");
                            if (tradeFile.exists()) {
                                FileReader tradeReader = new FileReader(tradeFile);
                                Trade currentTrade = JSON.std.beanFrom(Trade.class, tradeReader);
                                source.onNext(new TradeReceived(currentTrade, receivedTrade));
                            } else if (receivedTrade.status().equals(CREATED)) {
                                source.onNext(new TradeReceived(null, receivedTrade));
                            }
                        } catch (IOException ioe) {
                            source.onError(ioe);
                        }
                    }
                    source.onComplete();
                }
        );
    }

//    public Trade getSelectedTrade() {
//        return selectedTrade.get();
//    }

//    public ObjectProperty<Trade> selectedTradeProperty() {
//        return selectedTrade;
//    }

//    public Observable<TradeEvent> getTradeEvents() {
//        return tradeEvents;
//    }

//    public Observable<Trade> getUpdatedTrades() {
//        return updatedTrades;
//    }

//    public PublishSubject<TradeEvent> getCreatedTradeEvents() {
//        return createdTradeEvents;
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

    // Trade Action Classes

    public interface TradeAction extends Event {
    }

    public class LoadTrades implements TradeAction {
    }

    public class GetTrades implements TradeAction {
        private final Profile profile;

        public GetTrades(Profile profile) {
            this.profile = profile;
        }

        public Profile getProfile() {
            return profile;
        }
    }

    public class CreateTrade implements TradeAction {

        private final String escrowAddress;
        private final Trade.Role role;
        private final SellOffer sellOffer;
        private final BuyRequest buyRequest;

        public CreateTrade(String escrowAddress, Trade.Role role, SellOffer sellOffer, BuyRequest buyRequest) {
            this.escrowAddress = escrowAddress;
            this.role = role;
            this.sellOffer = sellOffer;
            this.buyRequest = buyRequest;
        }

        public String getEscrowAddress() {
            return escrowAddress;
        }

        public Trade.Role getRole() {
            return role;
        }

        public SellOffer getSellOffer() {
            return sellOffer;
        }

        public BuyRequest getBuyRequest() {
            return buyRequest;
        }
    }

    public class SelectTrade implements TradeAction {
        private final Trade trade;

        public SelectTrade(Trade trade) {
            this.trade = trade;
        }

        public Trade getTrade() {
            return trade;
        }
    }

    // Trade Result Classes

    public interface TradeResult extends Result {
    }

    public class TradeCreated implements TradeResult {

        private final Trade trade;

        public TradeCreated(Trade trade) {
            this.trade = trade;
        }

        public Trade getTrade() {
            return trade;
        }
    }

    public class TradeWritten implements TradeResult {

        private final Trade trade;

        public TradeWritten(Trade trade) {
            this.trade = trade;
        }

        public Trade getTrade() {
            return trade;
        }
    }

    public class TradePut implements TradeResult {

        private final Trade trade;

        public TradePut(Trade trade) {
            this.trade = trade;
        }

        public Trade getTrade() {
            return trade;
        }
    }

    public class TradeLoaded implements TradeResult {

        private final Trade trade;

        public TradeLoaded(Trade trade) {
            this.trade = trade;
        }

        public Trade getTrade() {
            return trade;
        }
    }

    public class TradeReceived implements TradeResult {

        private final Trade currentTrade;
        private final Trade receivedTrade;

        public TradeReceived(Trade oldTrade, Trade newTrade) {
            this.currentTrade = oldTrade;
            this.receivedTrade = newTrade;
        }

        public Trade getCurrentTrade() {
            return currentTrade;
        }

        public Trade getReceivedTrade() {
            return receivedTrade;
        }
    }

    public class TradeSelected implements TradeResult {
        private final Trade trade;

        public TradeSelected(Trade trade) {
            this.trade = trade;
        }

        public Trade getTrade() {
            return trade;
        }
    }

    // Trade Error Class

    public class TradeError implements TradeResult, ErrorResult {

        private final Throwable error;

        public TradeError(Throwable error) {
            this.error = error;
        }

        public Throwable getError() {
            return error;
        }
    }
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