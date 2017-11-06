package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import org.bitcoinj.core.InsufficientMoneyException;
import org.slf4j.LoggerFactory;

import static com.bytabit.mobile.trade.model.PayoutCompleted.Reason.ARBITRATOR_BUYER_PAYOUT;
import static com.bytabit.mobile.trade.model.PayoutCompleted.Reason.ARBITRATOR_SELLER_REFUND;
import static com.bytabit.mobile.trade.model.Trade.Status.ARBITRATING;

public class ArbitratorProtocol extends TradeProtocol {

    public ArbitratorProtocol() {
        super(LoggerFactory.getLogger(ArbitratorProtocol.class));
    }

    @Override
    public Trade handleCreated(Trade createdTrade) {
        return null;
    }

    @Override
    public Trade handleFunded(Trade createdTrade, Trade fundedTrade) {
        return null;
    }

    @Override
    public Trade handlePaid(Trade fundedTrade, Trade paidTrade) {
        return null;
    }

    @Override
    public Trade handleArbitrating(Trade currentTrade, Trade arbitratingTrade) {

        // TODO handle unexpected status
        if (currentTrade == null) {

            walletManager.createEscrowWallet(arbitratingTrade.getEscrowAddress());
            walletManager.resetBlockchain();

            return arbitratingTrade;
        } else {
            return null;
        }
    }

    @Override
    public Trade handleCompleted(Trade currentTrade, Trade completedTrade) {
        if (completedTrade.getPayoutReason().equals(ARBITRATOR_BUYER_PAYOUT) ||
                completedTrade.getPayoutReason().equals(ARBITRATOR_SELLER_REFUND)) {
            return super.handleCompleted(currentTrade, completedTrade);
        } else {
            return completedTrade;
        }
    }

    public void refundSeller(Trade currentTrade) {

        if (currentTrade.status().equals(ARBITRATING)) {

            String payoutTxHash = null;
            try {
                payoutTxHash = walletManager.refundEscrowToSeller(currentTrade);
                completeTrade(currentTrade, payoutTxHash, ARBITRATOR_SELLER_REFUND);

            } catch (InsufficientMoneyException e) {
                // TODO notify user
                log.error("Insufficient funds to refund escrow to seller.", e);
            }
        }
    }

    public void payoutBuyer(Trade currentTrade) {

        if (currentTrade.status().equals(ARBITRATING)) {

            String payoutTxHash = null;
            try {
                payoutTxHash = walletManager.payoutEscrowToBuyer(currentTrade);
                completeTrade(currentTrade, payoutTxHash, ARBITRATOR_BUYER_PAYOUT);

            } catch (InsufficientMoneyException e) {
                // TODO notify user
                log.error("Insufficient funds to refund escrow to seller.", e);
            }
        }
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

            tradeService.put(completedTrade.getEscrowAddress(), completedTrade).subscribe();

//            } catch (IOException e) {
//                log.error("Unable to put completed arbitrated trade.", e);
//            }
        }
    }
}
