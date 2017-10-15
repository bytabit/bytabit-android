package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import org.bitcoinj.core.InsufficientMoneyException;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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

    public void refundSeller(Trade currentTrade) {

        if (currentTrade.getStatus().equals(ARBITRATING)) {

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

        if (currentTrade.getStatus().equals(ARBITRATING)) {

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

    private void completeTrade(Trade currentTrade, String payoutTxHash, PayoutCompleted.Reason reason) {

        if (payoutTxHash != null) {

            // 2. confirm refund tx and create payout completed
            PayoutCompleted payoutCompleted = PayoutCompleted.builder()
                    .payoutTxHash(payoutTxHash)
                    .reason(reason)
                    .build();

            try {
                tradeService.put(currentTrade.getEscrowAddress(), payoutCompleted).execute();

            } catch (IOException e) {
                log.error("Unable to put completed arbitrated trade.", e);
            }
        }
    }
}
