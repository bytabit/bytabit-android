package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.profile.manager.PaymentDetailsManager;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.trade.model.ArbitrateRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import io.reactivex.Maybe;

import javax.inject.Inject;

public abstract class TradeProtocol {

    @Inject
    WalletManager walletManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    PaymentDetailsManager paymentDetailsManager;

    // CREATED, *FUNDING*, FUNDED, PAID, *COMPLETING*, COMPLETED, ARBITRATING

    abstract public Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade);

    abstract public Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade);

    public Maybe<Trade> handleUpdatedEscrowTx(Trade trade, TransactionWithAmt transactionWithAmt) {

        if (trade.getFundingTxHash() != null && trade.getFundingTxHash().equals(transactionWithAmt.getHash())) {

            return Maybe.just(trade)
                    .filter(t -> transactionWithAmt.getTransactionBigDecimalAmt().subtract(walletManager.defaultTxFee()).compareTo(t.getBtcAmount()) == 0)
                    .doOnSuccess(t -> {
                        if (transactionWithAmt.getDepth() > 0) {
                            t.fundingTransactionWithAmt(transactionWithAmt);
                        }
                    });
        } else if (trade.getPayoutTxHash() != null && trade.getPayoutTxHash().equals(transactionWithAmt.getHash())) {

            return Maybe.just(trade)
                    .filter(t -> transactionWithAmt.getTransactionBigDecimalAmt().compareTo(t.getBtcAmount()) == 0)
                    .doOnSuccess(t -> {
                        if (transactionWithAmt.getDepth() > 0) {
                            t.payoutTransactionWithAmt(transactionWithAmt);
                        }
                    });
        } else {
            return Maybe.empty();
        }
    }

    public Maybe<Trade> handlePaid(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();

        if (receivedTrade.hasArbitrateRequest()) {
            trade.arbitrateRequest(receivedTrade.arbitrateRequest());
            updatedTrade = Maybe.just(trade);
        }

        if (receivedTrade.hasPayoutCompleted()) {
            trade.payoutCompleted(receivedTrade.payoutCompleted());
            updatedTrade = Maybe.just(trade);
        }

        return updatedTrade;
    }

//    public Maybe<Trade> handleCompleting(Trade currentTrade, Trade completing) {
//
//        return Maybe.just(currentTrade)
//                .filter(t -> transactionWithAmt.getHash().equals(t.getFundingTxHash()))
//                .filter(t -> transactionWithAmt.getTransactionBigDecimalAmt().subtract(walletManager.defaultTxFee()).compareTo(t.getBtcAmount()) == 0)
//                .doOnSuccess(t -> {
//                    if (transactionWithAmt.getDepth() > 0) {
//                        t.fundingTransactionWithAmt(transactionWithAmt);
//                    }
//                });

//        // TODO refactor to use Observable
//        Profile profile = profileManager.loadOrCreateMyProfile().observeOn(JavaFxScheduler.platform()).blockingGet();

//        Maybe<Trade> verifiedCompletedTrade = Maybe.empty();

    // confirm payout tx
//        String txHash = completing.getPayoutTxHash();
//        String toAddress = completing.getBuyerPayoutAddress();
//
//        // if arbitrated refund to seller, verify outputs to refund address
//        if (completing.getPayoutReason().equals(ARBITRATOR_SELLER_REFUND) ||
//                completing.getPayoutReason().equals(BUYER_SELLER_REFUND)) {
//            toAddress = completing.getRefundAddress();
//        }

//        Boolean zeroConfOK = false;
//        PayoutCompleted.Reason payoutReason = completing.getPayoutReason();
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

//        return verifiedCompletedTrade;
//    }

    protected void requestArbitrate(Trade trade, ArbitrateRequest.Reason reason) {

//        if (trade.status().equals(FUNDED) || trade.status().equals(PAID)) {

        ArbitrateRequest arbitrateRequest = new ArbitrateRequest(reason);

        Trade arbitratingTrade = Trade.builder()
                .escrowAddress(trade.getEscrowAddress())
                .sellOffer(trade.sellOffer())
                .buyRequest(trade.buyRequest())
                .paymentRequest(trade.paymentRequest())
                .payoutRequest(trade.payoutRequest())
                .arbitrateRequest(arbitrateRequest)
                .build();

//            tradeService.put(arbitratingTrade).subscribe();
//        }
    }

    public Maybe<Trade> handleArbitrating(Trade trade, Trade receivedTrade) {

        // TODO handle unexpected status
//        if (trade.status().equals(FUNDED) || trade.status().equals(PAID)) {
//            return Maybe.just(receivedTrade);
//        } else {
        return Maybe.just(trade);
//        }
    }
}
