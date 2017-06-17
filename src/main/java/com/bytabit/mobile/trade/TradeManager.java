package com.bytabit.mobile.trade;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.model.*;
import com.bytabit.mobile.wallet.EscrowWalletManager;
import com.bytabit.mobile.wallet.TradeWalletManager;
import com.bytabit.mobile.wallet.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import static com.bytabit.mobile.trade.model.Trade.Status.*;

public class TradeManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(TradeManager.class);

    private final BuyRequestService buyRequestService;

    private final PaymentRequestService paymentRequestService;

    private final PayoutRequestService payoutRequestService;

    private final PayoutCompletedService payoutCompletedService;

    private final ObservableList<Trade> tradesObservableList;

    private final Trade viewTrade;

    @Inject
    ProfileManager profileManager;

    @Inject
    TradeWalletManager tradeWalletManager;

    @Inject
    EscrowWalletManager escrowWalletManager;

    String tradesPath = AppConfig.getPrivateStorage().getPath() + File.separator +
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

        tradesObservableList = FXCollections.observableArrayList();
        viewTrade = new Trade();

        readTrades();
        updateTrades();
    }

    // 1.B: buyer create buy request, create trade, write sell offer + buy request, post buy request
    public void createBuyRequest(SellOffer sellOffer, BigDecimal buyBtcAmount,
                                 String buyerEscrowPubKey, String buyerProfilePubKey,
                                 String buyerPayoutAddress) {

        // 1. create buy request
        BuyRequest buyRequest = new BuyRequest(sellOffer.getSellerEscrowPubKey(),
                buyerEscrowPubKey, buyBtcAmount, buyerProfilePubKey, buyerPayoutAddress);

        // 2. create trade
        Trade trade = createTrade(sellOffer, buyRequest);

        if (!tradesObservableList.contains(trade)) {

            // 3. watch trade escrow address
            escrowWalletManager.addWatchedEscrowAddress(trade.getEscrowAddress());

            // 4. write trade
            writeTrade(trade);

            // 5. post buy request to server
            try {
                String spk = sellOffer.getSellerEscrowPubKey();
                BuyRequest postedBuyRequest = buyRequestService.post(spk, buyRequest).execute().body();
                LOG.debug("Posted buyRequest: {}", postedBuyRequest);
            } catch (IOException ioe) {
                LOG.error(ioe.getMessage());
                throw new RuntimeException(ioe);
            }

            // 6. add trade to ui list
            tradesObservableList.add(trade);
            LOG.debug("Added trade to list: {}", trade);
        }
    }

    // 1.S: seller receives buy request, create and write trade with sell offer + buy request
    public void receiveBuyRequest(SellOffer sellOffer, BuyRequest buyRequest) {

        // 1. create trade
        Trade trade = createTrade(sellOffer, buyRequest);

        if (!tradesObservableList.contains(trade)) {

            // 2. watch trade escrow address
            escrowWalletManager.addWatchedEscrowAddress(trade.getEscrowAddress());

            // 3. write sell offer + buy request to trade folder
            writeTrade(trade);

            // 4. fund escrow
            fundEscrow(trade);

            // 5. add trade to ui list
            tradesObservableList.add(trade);
            LOG.debug("Added trade to list: {}", trade);
        }
    }

    private Trade createTrade(SellOffer sellOffer, BuyRequest buyRequest) {
        String tradeEscrowAddress = WalletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(), sellOffer.getSellerEscrowPubKey(), buyRequest.getBuyerEscrowPubKey());
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
    private void fundEscrow(Trade trade) {
        // TODO verify escrow not yet funded ?
        try {
            // 1. fund escrow
            String txHash = tradeWalletManager.fundEscrow(trade.getEscrowAddress(),
                    trade.getBuyRequest().getBtcAmount());

            // 2. create payment request
            String paymentDetails = profileManager.retrievePaymentDetails(trade.getSellOffer()
                    .getCurrencyCode(), trade.getSellOffer().getPaymentMethod()).get();
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setEscrowAddress(trade.getEscrowAddress());
            paymentRequest.setFundingTxHash(txHash);
            paymentRequest.setPaymentDetails(paymentDetails);

            // 3. write payment request to trade folder
            writePaymentRequest(trade, paymentRequest);

            // 4. post payment request
            try {
                paymentRequestService.post(trade.getEscrowAddress(), paymentRequest).execute().body();
            } catch (IOException e) {
                LOG.error("Unable to POST payment request.");
                // TODO retry posting payment request
            }

            // 5. update trade with payment request
            trade.setPaymentRequest(paymentRequest);

        } catch (InsufficientMoneyException e) {
            LOG.error("Insufficient BTC to fund trade escrow.");
            // TODO let user know not enough BTC in wallet
        }
    }

    // 2.B: buyer receives payment request, confirm funding tx, write payment request
    public void receivePaymentRequest(PaymentRequest paymentRequest) {

        // 1. buyer confirm funding tx
        TransactionWithAmt tx = escrowWalletManager.getTransactionWithAmt(paymentRequest.getFundingTxHash());
        Trade trade = getTrade(paymentRequest.getEscrowAddress());

        if (tx != null && trade.getBuyRequest().getBtcAmount().add(tradeWalletManager.defaultTxFee()).equals(tx.getBtcAmt())) {

            // 2. write payment request to trade folder
            writePaymentRequest(paymentRequest);

            // 3. update view and list trade with payment request
            viewTrade.setPaymentRequest(paymentRequest);
            trade.setPaymentRequest(paymentRequest);
        } else {
            LOG.error("Payment request funding tx btc amount doesn't match buy offer btc amount.");
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
        payoutRequest.setEscrowAddress(viewTrade.getEscrowAddress());
        payoutRequest.setPaymentReference(paymentReference);
        String fundingTxHash = viewTrade.getPaymentRequest().getFundingTxHash();
        Transaction fundingTx = escrowWalletManager.getTransaction(fundingTxHash);
        String payoutSignature = tradeWalletManager.getPayoutSignature(viewTrade, fundingTx);
        payoutRequest.setPayoutTxSignature(payoutSignature);

        // 2. write payout request to trade folder
        writePayoutRequest(payoutRequest);

        // 3. post payout request to server
        try {
            payoutRequestService.post(viewTrade.getEscrowAddress(), payoutRequest).execute().body();
        } catch (IOException e) {
            LOG.error("Can't post payout request to server.");
        }

        // 4. update view and list trade with payoutRequest
        viewTrade.setPayoutRequest(payoutRequest);
        getTrade(viewTrade.getEscrowAddress()).setPayoutRequest(payoutRequest);
    }

    // 3.S: seller receives payout request from buyer
    public void receivePayoutRequest(PayoutRequest payoutRequest) {

        Trade trade = getTrade(payoutRequest.getEscrowAddress());

        if (trade.getPayoutRequest() == null) {
            // 1. write payout request to trade folder
            writePayoutRequest(payoutRequest);

            // 2. update view and list trade with payoutRequest
            viewTrade.setPayoutRequest(payoutRequest);
            getTrade(viewTrade.getEscrowAddress()).setPayoutRequest(payoutRequest);
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
        String payoutTxHash = payoutEscrow();

        // 2. confirm payout tx and create payout completed
        PayoutCompleted payoutCompleted = new PayoutCompleted();
        payoutCompleted.setEscrowAddress(viewTrade.getEscrowAddress());
        payoutCompleted.setPayoutTxHash(payoutTxHash);

        // 3. write payout details to trade folder
        writePayoutCompleted(viewTrade, payoutCompleted);

        // 4. post payout completed
        try {
            payoutCompletedService.post(payoutCompleted.getEscrowAddress(), payoutCompleted).execute().body();
        } catch (IOException e) {
            LOG.error("Can't post payout completed to server.");
        }

        // 5. update trade
        viewTrade.setPayoutCompleted(payoutCompleted);
        getTrade(viewTrade.getEscrowAddress()).setPayoutCompleted(payoutCompleted);

        // 6. remove watch on escrow address
        escrowWalletManager.removeWatchedEscrowAddress(payoutCompleted.getEscrowAddress());
    }

    private String payoutEscrow() {

        String fundingTxHash = viewTrade.getPaymentRequest().getFundingTxHash();
        TransactionWithAmt fundingTxWithAmt = tradeWalletManager.getTransactionWithAmt(fundingTxHash);

        String payoutTx = null;
        if (fundingTxWithAmt != null) {
            try {
                String signature = tradeWalletManager.getPayoutSignature(viewTrade, fundingTxHash);
                payoutTx = escrowWalletManager.payoutEscrow(viewTrade, fundingTxHash, signature);

            } catch (InsufficientMoneyException e) {
                // TODO notify user
                LOG.error("Insufficient funds to payout escrow to buyer.");
            }

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
        TransactionWithAmt tx = tradeWalletManager.getTransactionWithAmt(payoutCompleted.getPayoutTxHash());

        if (trade != null && tx != null && tx.getBtcAmt().equals(trade.getBuyRequest().getBtcAmount())) {

            // 2. write payout details to trade folder
            writePayoutCompleted(trade, payoutCompleted);

            // 3. update trade
            viewTrade.setPayoutCompleted(payoutCompleted);
            trade.setPayoutCompleted(payoutCompleted);

            // 4. remove watch on escrow address
            escrowWalletManager.removeWatchedEscrowAddress(trade.getEscrowAddress());
        }

    }


//    private void addTrade(Trade trade) {
//
//        TradeRole role = getTradeRole(trade);
//        Trade.Status status = trade.getStatus();
//
//        // Add or Remove Watched Escrow Address
//        if (status != COMPLETED) {
//            escrowWalletManager.addWatchedEscrowAddress(trade.getEscrowAddress());
//        } else {
//            escrowWalletManager.removeWatchedEscrowAddress(trade.getEscrowAddress());
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
                    for (BuyRequest buyRequest : buyRequests) {
                        receiveBuyRequest(sellOffer, buyRequest);
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

    public Trade getViewTrade() {
        return viewTrade;
    }

    public void updateTrades() {

        Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> tradesObservableList)
                .flatMapIterable(tradeList -> tradeList)
                .filter(trade -> trade.getStatus().equals(CREATED))
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

        Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> tradesObservableList)
                .flatMapIterable(tradeList -> tradeList)
                .filter(trade -> trade.getStatus().equals(FUNDED))
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

        Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> tradesObservableList)
                .flatMapIterable(tradeList -> tradeList)
                .filter(trade -> trade.getStatus().equals(PAID))
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
            Trade trade = new Trade();
            trade.setEscrowAddress(tradeId);
            try {
                File sellOfferFile = new File(tradesPath + tradeId + File.separator + "sellOffer.json");
                FileReader sellOfferReader = new FileReader(sellOfferFile);
                SellOffer sellOffer = JSON.std.beanFrom(SellOffer.class, sellOfferReader);
                trade.setSellOffer(sellOffer);

                File buyRequestFile = new File(tradesPath + tradeId + File.separator + "buyRequest.json");
                FileReader buyRequestReader = new FileReader(buyRequestFile);
                BuyRequest buyRequest = JSON.std.beanFrom(BuyRequest.class, buyRequestReader);
                trade.setBuyRequest(buyRequest);

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
//                escrowWalletManager.removeWatchedEscrowAddress(trade.getEscrowAddress());
//            }
//        }
//    }
}