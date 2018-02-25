package com.bytabit.mobile.trade;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.trade.evt.BuyerCreated;
import com.bytabit.mobile.trade.model.ArbitrateRequest;
import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import org.slf4j.Logger;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import javax.inject.Inject;

import static com.bytabit.mobile.trade.model.PayoutCompleted.Reason.ARBITRATOR_SELLER_REFUND;
import static com.bytabit.mobile.trade.model.PayoutCompleted.Reason.BUYER_SELLER_REFUND;
import static com.bytabit.mobile.trade.model.Trade.Status.FUNDED;
import static com.bytabit.mobile.trade.model.Trade.Status.PAID;

public abstract class TradeProtocol {

    protected final Logger log;

    @Inject
    protected WalletManager walletManager;

    @Inject
    protected ProfileManager profileManager;

    protected final TradeService tradeService;

    protected TradeProtocol(Logger log) {

        this.log = log;

        Retrofit tradeRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(new JacksonJrConverter<>(Trade.class))
                .build();
        tradeService = tradeRetrofit.create(TradeService.class);
    }

    // CREATED, *FUNDING*, FUNDED, PAID, *COMPLETING*, COMPLETED, ARBITRATING


    abstract public Trade handleCreated(BuyerCreated created);

    abstract public Trade handleFunded(Trade createdTrade, Trade fundedTrade);

    public Trade handlePaid(Trade fundedTrade, Trade paidTrade) {

        // TODO handle unexpected status
        if (fundedTrade.status().equals(FUNDED)) {
            return paidTrade;
        } else {
            return null;
        }
    }

    public Trade handleCompleted(Trade currentTrade, Trade completedTrade) {

//        // TODO refactor to use Observable
//        Profile profile = profileManager.loadMyProfile().observeOn(JavaFxScheduler.platform()).blockingGet();

        Trade verifiedCompletedTrade = null;

        // confirm payout tx
        String txHash = completedTrade.getPayoutTxHash();
        String toAddress = completedTrade.getBuyerPayoutAddress();

        // if arbitrated refund to seller, verify outputs to refund address
        if (completedTrade.getPayoutReason().equals(ARBITRATOR_SELLER_REFUND) ||
                completedTrade.getPayoutReason().equals(BUYER_SELLER_REFUND)) {
            toAddress = completedTrade.getRefundAddress();
        }

        Boolean zeroConfOK = false;
        PayoutCompleted.Reason payoutReason = completedTrade.getPayoutReason();
//        Trade.Role tradeRole = completedTrade.role(profile.getPubKey(), profile.getIsArbitrator());
//        if ((payoutReason.equals(SELLER_BUYER_PAYOUT) && tradeRole.equals(Trade.Role.SELLER)) ||
//                (payoutReason.equals(BUYER_SELLER_REFUND) && tradeRole.equals(Trade.Role.BUYER)) ||
//                tradeRole.equals(Trade.Role.ARBITRATOR)) {
//
//            zeroConfOK = true;
//        }

//        TransactionWithAmt tx = walletManager.getTransactionWithAmt(completedTrade.getEscrowAddress(), txHash, toAddress);
//        if (tx != null) {
//            if (tx.getTransactionBigDecimalAmt().compareTo(completedTrade.getBtcAmount()) == 0) {
//                if (zeroConfOK || tx.getDepth() > 0) {
//
//                    verifiedCompletedTrade = completedTrade;
//
//                    // remove watch on escrow and refund addresses
//                    walletManager.removeWatchedEscrowAddress(completedTrade.getEscrowAddress());
//
//                } else {
//                    log.info("PayoutCompleted Tx depth not yet greater than 1.");
//                }
//            } else {
//                log.error("Tx amount wrong for PayoutCompleted.");
//            }
//        } else {
//            log.error("Tx not found for PayoutCompleted.");
//        }

        return verifiedCompletedTrade;
    }

    protected void requestArbitrate(Trade currentTrade, ArbitrateRequest.Reason reason) {

        if (currentTrade.status().equals(FUNDED) || currentTrade.status().equals(PAID)) {

            ArbitrateRequest arbitrateRequest = new ArbitrateRequest(reason);

            Trade arbitratingTrade = Trade.builder()
                    .escrowAddress(currentTrade.getEscrowAddress())
                    .sellOffer(currentTrade.sellOffer())
                    .buyRequest(currentTrade.buyRequest())
                    .paymentRequest(currentTrade.paymentRequest())
                    .payoutRequest(currentTrade.payoutRequest())
                    .arbitrateRequest(arbitrateRequest)
                    .build();

//            try {
            tradeService.put(currentTrade.getEscrowAddress(), arbitratingTrade).subscribe();
//            } catch (IOException e) {
//                log.error("Can't post ArbitrateRequest to server.");
//            }
        }
    }

    public Trade handleArbitrating(Trade currentTrade, Trade arbitratingTrade) {

        // TODO handle unexpected status
        if (currentTrade.status().equals(FUNDED) || currentTrade.status().equals(PAID)) {
            return arbitratingTrade;
        } else {
            return null;
        }
    }
}
