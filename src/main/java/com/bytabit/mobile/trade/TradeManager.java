package com.bytabit.mobile.trade;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.model.*;
import com.bytabit.mobile.wallet.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import rx.Observable;
import rx.schedulers.JavaFxScheduler;
import rx.schedulers.Schedulers;

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bytabit.mobile.trade.model.ArbitrateRequest.Reason.NO_BTC;
import static com.bytabit.mobile.trade.model.ArbitrateRequest.Reason.NO_PAYMENT;
import static com.bytabit.mobile.trade.model.Trade.Status.*;

public class TradeManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(TradeManager.class);

    @Inject
    ProfileManager profileManager;

    @Inject
    WalletManager walletManager;

    private final BuyRequestService buyRequestService;

    private final PaymentRequestService paymentRequestService;

    private final PayoutRequestService payoutRequestService;

    private final PayoutCompletedService payoutCompletedService;

    private final ArbitrateRequestService arbitrateRequestService;

    private final TradeService tradeService;

    private final ObservableList<Trade> tradesObservableList;

    private Trade selectedTrade;

    private String tradesPath = AppConfig.getPrivateStorage().getPath() + File.separator +
            "trades" + File.separator;

    public TradeManager() {
        Retrofit buyRequestRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(BuyRequest.class))
                .build();
        buyRequestService = buyRequestRetrofit.create(BuyRequestService.class);

        Retrofit paymentRequestRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(PaymentRequest.class))
                .build();
        paymentRequestService = paymentRequestRetrofit.create(PaymentRequestService.class);

        Retrofit payoutRequestRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(PayoutRequest.class))
                .build();
        payoutRequestService = payoutRequestRetrofit.create(PayoutRequestService.class);

        Retrofit payoutCompletedRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(PayoutCompleted.class))
                .build();
        payoutCompletedService = payoutCompletedRetrofit.create(PayoutCompletedService.class);

        Retrofit arbitrateRequestRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(ArbitrateRequest.class))
                .build();
        arbitrateRequestService = arbitrateRequestRetrofit.create(ArbitrateRequestService.class);

        Retrofit tradeRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(Trade.class))
                .build();
        tradeService = tradeRetrofit.create(TradeService.class);

        tradesObservableList = FXCollections.observableArrayList();

        readTrades();
        updateTrades();
    }

    // 1.B: buyer create buy request, create trade, write sell offer + buy request, post buy request
    public void createBuyRequest(SellOffer sellOffer, BigDecimal buyBtcAmount,
                                 String buyerEscrowPubKey, String buyerProfilePubKey,
                                 String buyerPayoutAddress) {

        // 1. create buy request
        String tradeEscrowAddress = WalletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(), sellOffer.getSellerEscrowPubKey(), buyerEscrowPubKey);
        BuyRequest buyRequest = new BuyRequest(tradeEscrowAddress, sellOffer.getSellerEscrowPubKey(),
                buyerEscrowPubKey, buyBtcAmount, buyerProfilePubKey, buyerPayoutAddress);

        // 2. create trade
        Trade trade = createTrade(sellOffer, buyRequest);

        if (!tradesObservableList.contains(trade)) {

            // 3. watch trade escrow address
            walletManager.addWatchedEscrowAddress(trade.getEscrowAddress());

            // 4. write trade
            writeTrade(trade);

            // 5. add trade to ui list
            tradesObservableList.add(trade);
            LOG.debug("Added trade to list: {}", trade);

            // 6. post buy request to server
            try {
                String spk = sellOffer.getSellerEscrowPubKey();
                BuyRequest postedBuyRequest = buyRequestService.post(spk, buyRequest).execute().body();
                LOG.debug("Posted buyRequest: {}", postedBuyRequest);
            } catch (IOException ioe) {
                LOG.error(ioe.getMessage());
                throw new RuntimeException(ioe);
            }
        }
    }

    // 1.S: seller receives buy request, create and write trade with sell offer + buy request
    public void receiveBuyRequest(SellOffer sellOffer, BuyRequest buyRequest) {

        // 1. create trade
        Trade trade = createTrade(sellOffer, buyRequest);

        if (!tradesObservableList.contains(trade)) {

            // 2. watch trade escrow address
            walletManager.addWatchedEscrowAddress(trade.getEscrowAddress());

            // 3. write sell offer + buy request to trade folder
            writeTrade(trade);

            // 4. fund escrow
            PaymentRequest paymentRequest = fundEscrow(trade);

            if (paymentRequest != null) {

                // 5. update trade with payment request
                trade.setPaymentRequest(paymentRequest);

                // 6. write payment request to trade folder
                writePaymentRequest(trade, paymentRequest);

                // 7. add trade to ui list
                tradesObservableList.add(trade);
                LOG.debug("Added trade to list: {}", trade);

                // 8. post payment request
                try {
                    paymentRequestService.post(trade.getEscrowAddress(), paymentRequest).execute().body();
                } catch (IOException e) {
                    LOG.error("Unable to POST payment request.");
                    // TODO retry posting payment request
                }
            }
        }
    }

    private Trade createTrade(SellOffer sellOffer, BuyRequest buyRequest) {
        String tradeEscrowAddress = WalletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(), sellOffer.getSellerEscrowPubKey(), buyRequest.getBuyerEscrowPubKey());
        if (!tradeEscrowAddress.equals(buyRequest.getEscrowAddress())) {
            LOG.error("Buyer and seller computed different escrow addresses for a trade.");
            // TODO this should never happen, but need to handle it better
            throw new RuntimeException("Buyer and seller computed different escrow addresses for a trade.");
        }
        return new Trade(sellOffer, buyRequest, tradeEscrowAddress);
    }

    private void writeTrade(Trade trade) {

        try {
            String tradePath = tradesPath + trade.getEscrowAddress();
            File tradeDir = new File(tradePath);

            if (!tradeDir.exists()) {
                tradeDir.mkdirs();
                String sellOfferPath = tradePath + File.separator + "sellOffer.json";
                FileWriter sellOfferFileWriter = new FileWriter(sellOfferPath);
                sellOfferFileWriter.write(JSON.std.asString(trade.getSellOffer()));
                sellOfferFileWriter.flush();
                String buyRequestPath = tradePath + File.separator + "buyRequest.json";
                FileWriter buyRequestFileWriter = new FileWriter(buyRequestPath);
                buyRequestFileWriter.write(JSON.std.asString(trade.getBuyRequest()));
                buyRequestFileWriter.flush();
                LOG.debug("Created new trade: {}", trade);
            }

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    // 2.S: seller fund escrow and post payment request
    private PaymentRequest fundEscrow(Trade trade) {
        // TODO verify escrow not yet funded ?
        try {
            // 1. fund escrow
            Transaction fundingTx = walletManager.fundEscrow(trade.getEscrowAddress(),
                    trade.getBuyRequest().getBtcAmount());

            // 2. create refund tx address and signature

            Address refundTxAddress = walletManager.getDepositAddress();
            String refundTxSignature = walletManager.getRefundSignature(trade, refundTxAddress, fundingTx);

            // 3. create payment request
            String paymentDetails = profileManager.retrievePaymentDetails(trade.getSellOffer()
                    .getCurrencyCode(), trade.getSellOffer().getPaymentMethod()).get();
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setEscrowAddress(trade.getEscrowAddress());
            paymentRequest.setFundingTxHash(fundingTx.getHashAsString());
            paymentRequest.setPaymentDetails(paymentDetails);
            paymentRequest.setRefundAddress(refundTxAddress.toBase58());
            paymentRequest.setRefundTxSignature(refundTxSignature);

            return paymentRequest;

        } catch (InsufficientMoneyException e) {
            LOG.error("Insufficient BTC to fund trade escrow.");
            // TODO let user know not enough BTC in wallet
            return null;
        }
    }

    // 2.B: buyer receives payment request, confirm funding tx, write payment request
    public void receivePaymentRequest(PaymentRequest paymentRequest) {

        if (paymentRequest != null) {
            // 1. buyer confirm funding tx
            TransactionWithAmt tx = walletManager.getEscrowTransactionWithAmt(paymentRequest.getFundingTxHash());
            if (tx != null) {
                Trade trade = getTrade(paymentRequest.getEscrowAddress());

                if (trade != null && trade.getBuyRequest().getBtcAmount().add(walletManager.defaultTxFee()).equals(tx.getBtcAmt())) {

                    // 2. write payment request to trade folder
                    writePaymentRequest(paymentRequest);

                    // 3. update view and list trade with payment request
                    trade.setPaymentRequest(paymentRequest);
                } else {
                    LOG.error("Trade not found for payment request or funding tx btc amount doesn't match buy offer btc amount.");
                }
            } else {
                LOG.error("Tx not found for payment request.");
            }
        }
    }

    private PaymentRequest writePaymentRequest(Trade trade, PaymentRequest paymentRequest) {

        String tradePath = tradesPath + trade.getEscrowAddress();
        String paymentRequestPath = tradePath + File.separator + "paymentRequest.json";
        try {
            FileWriter paymentRequestWriter = new FileWriter(paymentRequestPath);
            paymentRequestWriter.write(JSON.std.asString(paymentRequest));
            paymentRequestWriter.flush();

            LOG.debug("Created paymentRequest: {}", paymentRequest);
            return paymentRequest;
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    // 3.B: buyer sends payment to seller and post payout request
    // * after manually send payment to seller

    public void requestPayout(String paymentReference) {

        // 1. create payout request with buyer payout signature
        PayoutRequest payoutRequest = new PayoutRequest();
        payoutRequest.setEscrowAddress(selectedTrade.getEscrowAddress());
        payoutRequest.setPaymentReference(paymentReference);

        String fundingTxHash = selectedTrade.getPaymentRequest().getFundingTxHash();
        Transaction fundingTx = walletManager.getEscrowTransaction(fundingTxHash);
        if (fundingTx != null) {
            String payoutSignature = walletManager.getPayoutSignature(selectedTrade, fundingTx);
            payoutRequest.setPayoutTxSignature(payoutSignature);

            // 2. write payout request to trade folder
            writePayoutRequest(payoutRequest);

            // 3. post payout request to server
            try {
                payoutRequestService.post(payoutRequest.getEscrowAddress(), payoutRequest).execute().body();
            } catch (IOException e) {
                LOG.error("Can't post payout request to server.");
            }

            // 4. update view and list trade with payoutRequest
            selectedTrade.setPayoutRequest(payoutRequest);
        } else {
            LOG.error("Trade not found for payout request.");
        }
    }

    // 3.S: seller receives payout request from buyer
    public void receivePayoutRequest(PayoutRequest payoutRequest) {

        if (payoutRequest != null) {
            Trade trade = getTrade(payoutRequest.getEscrowAddress());

            if (trade != null && trade.getPayoutRequest() == null) {
                // 1. write payout request to trade folder
                writePayoutRequest(payoutRequest);

                // 2. update view and list trade with payoutRequest
                getTrade(trade.getEscrowAddress()).setPayoutRequest(payoutRequest);
            }
        }
    }

    private void writePayoutRequest(PayoutRequest payoutRequest) {
        String tradePath = tradesPath + payoutRequest.getEscrowAddress();
        String payoutRequestPath = tradePath + File.separator + "payoutRequest.json";
        try {
            FileWriter payoutRequestWriter = new FileWriter(payoutRequestPath);
            payoutRequestWriter.write(JSON.std.asString(payoutRequest));
            payoutRequestWriter.flush();


            LOG.debug("Created payoutRequest: {}", payoutRequest);

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    // 4.S: seller payout escrow to buyer and write payout details
    public void confirmPaymentReceived() {

        // 1. sign and broadcast payout tx
        String payoutTxHash = payoutEscrow(selectedTrade);

        // 2. confirm payout tx and create payout completed
        PayoutCompleted payoutCompleted = new PayoutCompleted();
        payoutCompleted.setEscrowAddress(selectedTrade.getEscrowAddress());
        payoutCompleted.setPayoutTxHash(payoutTxHash);

        // 3. write payout details to trade folder
        writePayoutCompleted(selectedTrade, payoutCompleted);

        // 4. update trade
        selectedTrade.setPayoutCompleted(payoutCompleted);

        // 5. post payout completed
        try {
            payoutCompletedService.post(payoutCompleted.getEscrowAddress(), payoutCompleted).execute().body();
        } catch (IOException e) {
            LOG.error("Can't post payout completed to server.");
        }

        // 6. remove watch on escrow address
        walletManager.removeWatchedEscrowAddress(payoutCompleted.getEscrowAddress());
    }

    private String payoutEscrow(Trade trade) {

        String payoutTx = null;
        try {
            payoutTx = walletManager.payoutEscrow(trade);
        } catch (InsufficientMoneyException e) {
            // TODO notify user
            LOG.error("Insufficient funds to payout escrow to buyer.");
        }

        return payoutTx;
    }

    public void writePayoutCompleted(Trade trade, PayoutCompleted payoutCompleted) {

        String tradePath = tradesPath + trade.getEscrowAddress();
        String payoutCompletedPath = tradePath + File.separator + "payoutCompleted.json";
        try {
            FileWriter payoutCompletedWriter = new FileWriter(payoutCompletedPath);
            payoutCompletedWriter.write(JSON.std.asString(payoutCompleted));
            payoutCompletedWriter.flush();

            LOG.debug("Created payoutCompleted: {}", payoutCompletedPath);
            trade.setPayoutCompleted(payoutCompleted);

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    // 4.B: buyer confirm payout tx and write payout details
    public void receivePayoutCompleted(PayoutCompleted payoutCompleted) {

        // 1. confirm payout tx
        Trade trade = getTrade(payoutCompleted.getEscrowAddress());
        if (trade != null) {
            TransactionWithAmt tx = walletManager.getTradeTransactionWithAmt(payoutCompleted.getPayoutTxHash());
            if (tx != null) {
                if (tx.getBtcAmt().equals(trade.getBuyRequest().getBtcAmount())) {

                    // 2. write payout details to trade folder
                    writePayoutCompleted(trade, payoutCompleted);

                    // 3. update trade
                    getTrade(trade.getEscrowAddress()).setPayoutCompleted(payoutCompleted);

                    // 4. remove watch on escrow address
                    walletManager.removeWatchedEscrowAddress(trade.getEscrowAddress());
                } else {
                    LOG.error("Tx amount wrong for PayoutCompleted.");
                }
            } else {
                LOG.error("Tx not found for PayoutCompleted.");
            }
        } else {
            LOG.error("Trade not found for PayoutCompleted.");
        }
    }

    // 5.: receive ArbitrateRequest
    public void receiveArbitrateRequest(ArbitrateRequest arbitrateRequest) {

        // 1. confirm payout tx
        Trade trade = getTrade(arbitrateRequest.getEscrowAddress());
        if (trade != null) {

            // 2. write arbitrate request to trade folder
            writeArbitrateRequest(trade, arbitrateRequest);

            // 3. update trade
            trade.setArbitrateRequest(arbitrateRequest);

        } else {
            LOG.error("Trade not found for ArbitrateRequest.");
        }
    }

    public void writeArbitrateRequest(Trade trade, ArbitrateRequest arbitrateRequest) {

        String tradePath = tradesPath + trade.getEscrowAddress();
        String arbitrateRequestPath = tradePath + File.separator + "arbitrateRequest.json";
        try {
            FileWriter arbitrateRequestWriter = new FileWriter(arbitrateRequestPath);
            arbitrateRequestWriter.write(JSON.std.asString(arbitrateRequest));
            arbitrateRequestWriter.flush();

            LOG.debug("Created arbitrateRequest: {}", arbitrateRequestPath);

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }


//    private void addTrade(Trade trade) {
//
//        TradeRole role = getTradeRole(trade);
//        Trade.Status status = trade.getStatus();
//
//        // Add or Remove Watched Escrow Address
//        if (status != COMPLETED) {
//            walletManager.addWatchedEscrowAddress(trade.getEscrowAddress());
//        } else {
//            walletManager.removeWatchedEscrowAddress(trade.getEscrowAddress());
//        }
//
//        // Fund Escrow + Request Payment
//        if (status == CREATED && role == SELLER) {
//            fundEscrow(trade);
//        }
//    }

    private Trade getTrade(String escrowAddress) {
        Trade foundTrade = null;

        for (Trade trade : tradesObservableList) {
            if (trade.getEscrowAddress().equals(escrowAddress)) {
                foundTrade = trade;
                break;
            }
        }

        return foundTrade;
    }

    public PaymentRequest readPaymentRequest(Trade trade) {

        String tradePath = tradesPath + trade.getEscrowAddress();
        String paymentRequestPath = tradePath + File.separator + "paymentRequest.json";
        try {
            PaymentRequest readPaymentRequest =
                    paymentRequestService.get(trade.getEscrowAddress()).execute().body();
            LOG.debug("Read paymentRequest: {}", readPaymentRequest);

            FileWriter paymentRequestWriter = new FileWriter(paymentRequestPath);
            paymentRequestWriter.write(JSON.std.asString(readPaymentRequest));
            paymentRequestWriter.flush();

            trade.setPaymentRequest(readPaymentRequest);
            return readPaymentRequest;

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    public PayoutRequest readPayoutRequest(Trade trade) {

        String tradePath = tradesPath + trade.getEscrowAddress();
        String payoutRequestPath = tradePath + File.separator + "payoutRequest.json";
        try {
            PayoutRequest readPayoutRequest =
                    payoutRequestService.get(trade.getEscrowAddress()).execute().body();
            LOG.debug("Read payoutRequest: {}", readPayoutRequest);

            FileWriter payoutRequestWriter = new FileWriter(payoutRequestPath);
            payoutRequestWriter.write(JSON.std.asString(readPayoutRequest));
            payoutRequestWriter.flush();

            trade.setPayoutRequest(readPayoutRequest);
            return readPayoutRequest;

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    public void createTrades(String profilePubKey, List<? extends SellOffer> offers) {
        for (SellOffer sellOffer : offers) {
            try {
                if (sellOffer.getSellerProfilePubKey().equals(profilePubKey)) {
                    List<BuyRequest> buyRequests = buyRequestService.get(sellOffer.getSellerEscrowPubKey()).execute().body();
                    if (buyRequests != null) {
                        for (BuyRequest buyRequest : buyRequests) {
                            receiveBuyRequest(sellOffer, buyRequest);
                        }
                    }
                }
            } catch (IOException ioe) {
                LOG.error("Error getting buy requests for sell offer {}", sellOffer.getSellerEscrowPubKey());
            }
        }
    }

    public ObservableList<Trade> getTradesObservableList() {
        return tradesObservableList;
    }

    public void updateTrades() {

        Observable<Trade> trades = Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> getTradesObservableList())
                .flatMapIterable(tradeList -> tradeList).share();

        trades.filter(trade -> trade.getStatus().equals(CREATED))
                .map(trade -> paymentRequestService.get(trade.getEscrowAddress()))
                .retry()
                .observeOn(JavaFxScheduler.getInstance())
                .subscribe(c -> {
                    LOG.debug(c.toString());
                    try {
                        PaymentRequest paymentRequest = c.execute().body();
                        receivePaymentRequest(paymentRequest);
                    } catch (IOException ioe) {
                        LOG.error(ioe.getMessage());
                    }
                });

        trades.filter(trade -> trade.getStatus().equals(FUNDED))
                .map(trade -> payoutRequestService.get(trade.getEscrowAddress()))
                .retry()
                .observeOn(JavaFxScheduler.getInstance())
                .subscribe(c -> {
                    try {
                        PayoutRequest payoutRequest = c.execute().body();
                        receivePayoutRequest(payoutRequest);
                    } catch (IOException ioe) {
                        LOG.error(ioe.getMessage());
                    }
                });

        trades.filter(trade -> trade.getStatus().equals(PAID))
                .map(trade -> payoutCompletedService.get(trade.getEscrowAddress()))
                .retry()
                .observeOn(JavaFxScheduler.getInstance())
                .subscribe(c -> {
                    try {
                        PayoutCompleted payoutCompleted = c.execute().body();
                        receivePayoutCompleted(payoutCompleted);
                    } catch (IOException ioe) {
                        LOG.error(ioe.getMessage());
                    }
                });

        trades.filter(trade -> trade.getStatus().equals(FUNDED) ||
                trade.getStatus().equals(PAID))
                .map(trade -> arbitrateRequestService.get(
                        trade.getSellOffer().getArbitratorProfilePubKey(),
                        trade.getEscrowAddress()))
                .retry()
                .observeOn(JavaFxScheduler.getInstance())
                .subscribe(c -> {
                    try {
                        ArbitrateRequest arbitrateRequest = c.execute().body();
                        receiveArbitrateRequest(arbitrateRequest);
                    } catch (IOException ioe) {
                        LOG.error(ioe.getMessage());
                    }
                });

        Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> profileManager.profile().isIsArbitrator())
                .filter(b -> b.equals(true))
                .map(b -> tradeService.get(profileManager.profile().getPubKey()))
                .retry()
                .observeOn(JavaFxScheduler.getInstance())
                .subscribe(c -> {
                    try {
                        receiveArbitratorTrades(c.execute().body());
                    } catch (IOException ioe) {
                        LOG.error(ioe.getMessage());
                    }
                });
    }

//    private void updateTrade(PaymentRequest paymentRequest) {
//
//        writePaymentRequest(paymentRequest);
//        for (Trade trade : tradesObservableList) {
//            if (trade.getEscrowAddress().equals(paymentRequest.getEscrowAddress())) {
//                trade.setPaymentRequest(paymentRequest);
//            }
//        }
//    }

    public void writePaymentRequest(PaymentRequest paymentRequest) {

        String tradePath = tradesPath + paymentRequest.getEscrowAddress();
        String paymentRequestPath = tradePath + File.separator + "paymentRequest.json";

        try {
            FileWriter paymentRequestWriter = new FileWriter(paymentRequestPath);
            paymentRequestWriter.write(JSON.std.asString(paymentRequest));
            paymentRequestWriter.flush();

            LOG.debug("Created local paymentRequest: {}", paymentRequest);

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    private void readTrades() {

        // get stored trades

        File tradesDir = new File(tradesPath);
        List<String> tradeIds;
        if (tradesDir.list() != null) {
            tradeIds = Arrays.asList(tradesDir.list());
        } else {
            tradeIds = new ArrayList<>();
        }
        for (String tradeId : tradeIds) {

            try {
                File sellOfferFile = new File(tradesPath + tradeId + File.separator + "sellOffer.json");
                FileReader sellOfferReader = new FileReader(sellOfferFile);
                SellOffer sellOffer = JSON.std.beanFrom(SellOffer.class, sellOfferReader);

                File buyRequestFile = new File(tradesPath + tradeId + File.separator + "buyRequest.json");
                FileReader buyRequestReader = new FileReader(buyRequestFile);
                BuyRequest buyRequest = JSON.std.beanFrom(BuyRequest.class, buyRequestReader);

                Trade trade = new Trade(sellOffer, buyRequest, tradeId);

                File paymentRequestFile = new File(tradesPath + tradeId + File.separator + "paymentRequest.json");
                if (paymentRequestFile.exists()) {
                    FileReader paymentRequestReader = new FileReader(paymentRequestFile);
                    PaymentRequest paymentRequest = JSON.std.beanFrom(PaymentRequest.class, paymentRequestReader);
                    trade.setPaymentRequest(paymentRequest);
                }

                File payoutRequestFile = new File(tradesPath + tradeId + File.separator + "payoutRequest.json");
                if (payoutRequestFile.exists()) {
                    FileReader payoutRequestReader = new FileReader(payoutRequestFile);
                    PayoutRequest payoutRequest = JSON.std.beanFrom(PayoutRequest.class, payoutRequestReader);
                    trade.setPayoutRequest(payoutRequest);
                }

                File payoutCompletedFile = new File(tradesPath + tradeId + File.separator + "payoutCompleted.json");
                if (payoutCompletedFile.exists()) {
                    FileReader payoutCompletedReader = new FileReader(payoutCompletedFile);
                    PayoutCompleted payoutCompleted = JSON.std.beanFrom(PayoutCompleted.class, payoutCompletedReader);
                    trade.setPayoutCompleted(payoutCompleted);
                }

                File arbitrateRequestFile = new File(tradesPath + tradeId + File.separator + "arbitrateRequest.json");
                if (arbitrateRequestFile.exists()) {
                    FileReader arbitrateRequestReader = new FileReader(arbitrateRequestFile);
                    ArbitrateRequest arbitrateRequest = JSON.std.beanFrom(ArbitrateRequest.class, arbitrateRequestReader);
                    trade.setArbitrateRequest(arbitrateRequest);
                }

                tradesObservableList.add(trade);
            } catch (IOException ioe) {
                LOG.error(ioe.getMessage());
            }
        }
    }


    public boolean activeSellerEscrowPubKey(String sellerEscrowPubKey) {
        for (Trade trade : tradesObservableList) {
            // TODO check if in not COMPLETED status
            if (trade.getSellOffer().getSellerEscrowPubKey().equals(sellerEscrowPubKey) && trade.getStatus().equals(CREATED)) {
                return true;
            }
        }
        return false;
    }

    public boolean activeBuyerEscrowPubKey(String buyerEscrowPubKey) {
        for (Trade trade : tradesObservableList) {
            // TODO also check if in active status
            if (trade.getBuyRequest().getBuyerEscrowPubKey().equals(buyerEscrowPubKey)) {
                return true;
            }
        }
        return false;
    }

    public Trade getSelectedTrade() {
        return selectedTrade;
    }

    public void setSelectedTrade(Trade selectedTrade) {
        this.selectedTrade = selectedTrade;
    }

    //    public void updateTradeWithTx(TransactionWithAmt updatedTx) {
//        BigDecimal defaultTxFee = new BigDecimal(Transaction.DEFAULT_TX_FEE.toPlainString());
//        for (Trade trade : tradesObservableList) {
//            // 1. buyer confirm funding tx
//            if (trade.getEscrowAddress().equals(updatedTx.getOutputAddress())
//                    && trade.getBuyRequest().getBtcAmount().add(defaultTxFee).equals(updatedTx.getBtcAmt())
//                    && updatedTx.getConfidenceType().equals("BUILDING")
//                    && trade.getBuyRequest().getBuyerProfilePubKey().equals(profileManager.profile().getPubKey())) {
//
//
//            } else if (trade.getBuyRequest().getBuyerPayoutAddress().equals(updatedTx.getOutputAddress())
//                    && trade.getBuyRequest().getBtcAmount().equals(updatedTx.getBtcAmt())
//                    && trade.getPaymentRequest().getFundingTxHash().equals(updatedTx.getHash())
//                    && updatedTx.getConfidenceType().equals("BUILDING")) {
//
//                // post tradeCompleted.json
//                PayoutCompleted payoutCompleted = writePayoutCompleted(trade, updatedTx.getHash());
//
//                // stop watching escrow address
//                walletManager.removeWatchedEscrowAddress(trade.getEscrowAddress());
//            }
//        }
//    }

    // unhappy path

    private void receiveArbitratorTrades(List<Trade> trades) {
        tradesObservableList.setAll(trades);
    }

    public void requestArbitrate() {
        ArbitrateRequest arbitrateRequest = new ArbitrateRequest();
        arbitrateRequest.setArbitratorProfilePubKey(selectedTrade.getSellOffer().getArbitratorProfilePubKey());
        arbitrateRequest.setEscrowAddress(selectedTrade.getEscrowAddress());
        if (selectedTrade.getRole(profileManager.profile().getPubKey(), false).equals(Trade.Role.SELLER)) {
            arbitrateRequest.setReason(NO_PAYMENT);
        } else {
            arbitrateRequest.setReason(NO_BTC);
        }
        try {
            arbitrateRequestService.post(selectedTrade.getSellOffer().getArbitratorProfilePubKey(), arbitrateRequest).execute();
        } catch (IOException e) {
            LOG.error("Can't post ArbitrateRequest to server.");
        }
        writeArbitrateRequest(selectedTrade, arbitrateRequest);
    }

    public void refundSeller() {

    }

    public void payoutBuyer() {

    }
}