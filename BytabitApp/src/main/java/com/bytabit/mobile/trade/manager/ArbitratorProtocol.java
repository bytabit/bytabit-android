package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import io.reactivex.Observable;

public class ArbitratorProtocol extends TradeProtocol {

    public ArbitratorProtocol() {
        super();
    }

    @Override
    public Observable<Trade> handleCreated(Trade currentTrade, Trade createdTrade) {
        return Observable.empty();
    }

//    @Override
//    public Trade handleCreated(BuyerCreated created) {
//        return null;
//    }

    @Override
    public Observable<Trade> handleFunding(Trade currentTrade, Trade fundedTrade) {
        return Observable.empty();
    }

    @Override
    public Observable<Trade> handlePaid(Trade currentTrade, Trade paidTrade) {
        return Observable.empty();
    }

    @Override
    public Observable<Trade> handleArbitrating(Trade currentTrade, Trade arbitratingTrade) {

        // TODO handle unexpected status
//        if (currentTrade == null) {
//
//            walletManager.createEscrowWallet(arbitratingTrade.getEscrowAddress());
//            walletManager.resetBlockchain();
//
//            return arbitratingTrade;
//        } else {
//            return null;
//        }
        return Observable.empty();
    }

    @Override
    public Observable<Trade> handleCompleted(Trade currentTrade, Trade completedTrade) {

        // TODO don't do this way
        //Trade currentTrade = readTrade(completedTrade.getEscrowAddress()).blockingGet();

//        if (completedTrade.getPayoutReason().equals(ARBITRATOR_BUYER_PAYOUT) ||
//                completedTrade.getPayoutReason().equals(ARBITRATOR_SELLER_REFUND)) {
//            return super.handleCompleted(completedTrade);
//        } else {
//            return Observable.just(completedTrade);
//        }
        return null;
    }

    public void refundSeller(Trade currentTrade) {

//        if (currentTrade.status().equals(ARBITRATING)) {
//
//            String payoutTxHash = null;
//            try {
//                payoutTxHash = walletManager.refundEscrowToSeller(currentTrade);
//                completeTrade(currentTrade, payoutTxHash, ARBITRATOR_SELLER_REFUND);
//
//            } catch (InsufficientMoneyException e) {
//                // TODO notify user
//                log.error("Insufficient funds to refund escrow to seller.", e);
//            }
//        }
    }

    public void payoutBuyer(Trade currentTrade) {

//        if (currentTrade.status().equals(ARBITRATING)) {
//
//            String payoutTxHash = null;
//            try {
//                payoutTxHash = walletManager.payoutEscrowToBuyer(currentTrade);
//                completeTrade(currentTrade, payoutTxHash, ARBITRATOR_BUYER_PAYOUT);
//
//            } catch (InsufficientMoneyException e) {
//                // TODO notify user
//                log.error("Insufficient funds to refund escrow to seller.", e);
//            }
//        }
    }

    private void completeTrade(Trade arbitratingTrade, String payoutTxHash, PayoutCompleted.Reason reason) {

        if (payoutTxHash != null) {

            // 2. confirm refund tx and create payout completed
            PayoutCompleted payoutCompleted = new PayoutCompleted(payoutTxHash, reason);

//            try {

            Trade completedTrade = Trade.builder()
                    .escrowAddress(arbitratingTrade.getEscrowAddress())
                    .sellOffer(arbitratingTrade.sellOffer())
                    .buyRequest(arbitratingTrade.buyRequest())
                    .paymentRequest(arbitratingTrade.paymentRequest())
                    .payoutRequest(arbitratingTrade.payoutRequest())
                    .payoutCompleted(payoutCompleted)
                    .build();

//            tradeService.put(completedTrade).subscribe();

//            } catch (IOException e) {
//                log.error("Unable to put completed arbitrated trade.", e);
//            }
        }
    }
}
