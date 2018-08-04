package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.profile.manager.PaymentDetailsManager;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.trade.model.ArbitrateRequest;
import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import io.reactivex.Observable;

import javax.inject.Inject;

import static com.bytabit.mobile.trade.model.PayoutCompleted.Reason.ARBITRATOR_SELLER_REFUND;
import static com.bytabit.mobile.trade.model.PayoutCompleted.Reason.BUYER_SELLER_REFUND;
import static com.bytabit.mobile.trade.model.Trade.Status.FUNDED;
import static com.bytabit.mobile.trade.model.Trade.Status.PAID;

public abstract class TradeProtocol {

//    protected final Logger log;

    @Inject
    WalletManager walletManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    PaymentDetailsManager paymentDetailsManager;

//    protected final TradeService tradeService;

    protected TradeProtocol() {

//        tradeService = new TradeService();
    }

    // CREATED, *FUNDING*, FUNDED, PAID, *COMPLETING*, COMPLETED, ARBITRATING

    abstract public Observable<Trade> handleCreated(Trade createdTrade);

    abstract public Observable<Trade> handleFunded(Trade fundedTrade);

    public Observable<Trade> handleUpdatedEscrowTx(Trade currentTrade, TransactionWithAmt transactionWithAmt) {

        // if FUNDING transaction updated update trade status
        Observable<Trade> updatedTradeWithFundingTx = Observable.just(currentTrade)
                .filter(t -> transactionWithAmt.getHash().equals(t.getFundingTxHash()))
                .filter(t -> transactionWithAmt.getTransactionBigDecimalAmt().subtract(walletManager.defaultTxFee()).compareTo(t.getBtcAmount()) == 0)
                .doOnNext(t -> {
                    if (transactionWithAmt.getDepth() > 0) {
                        t.fundingTransactionWithAmt(transactionWithAmt);
                    }
                });

        return updatedTradeWithFundingTx;
    }

    public Observable<Trade> handlePaid(Trade fundedTrade, Trade paidTrade) {

        //Maybe<Trade> fundedTrade = readTrade(paidTrade.getEscrowAddress());

        // verify current trade is FUNDED, then return received PAID
        // TODO verify other properties of PAID trade match FUNDED trade
        return Observable.just(fundedTrade).filter(ft -> ft.status().equals(FUNDED))
                .map(ft -> paidTrade);
    }

    public Observable<Trade> handleCompleted(Trade currentTrade, Trade completedTrade) {

//        // TODO refactor to use Observable
//        Profile profile = profileManager.loadOrCreateMyProfile().observeOn(JavaFxScheduler.platform()).blockingGet();

        Observable<Trade> verifiedCompletedTrade = Observable.empty();

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
//        Trade.Role tradeRole = completedTrade.role(profile.getPubKey(), profile.isArbitrator());
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

//            tradeService.put(arbitratingTrade).subscribe();
        }
    }

    public Observable<Trade> handleArbitrating(Trade currentTrade, Trade arbitratingTrade) {

        // TODO handle unexpected status
        if (currentTrade.status().equals(FUNDED) || currentTrade.status().equals(PAID)) {
            return Observable.just(arbitratingTrade);
        } else {
            return Observable.empty();
        }
    }
}
