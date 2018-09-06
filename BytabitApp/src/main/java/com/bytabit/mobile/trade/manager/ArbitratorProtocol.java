package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import io.reactivex.Maybe;

public class ArbitratorProtocol extends TradeProtocol {

    public ArbitratorProtocol() {
        super();
    }

    @Override
    public Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();

        if (receivedTrade.hasArbitrateRequest()) {

            if (receivedTrade.hasPaymentRequest()) {
                trade.paymentRequest(receivedTrade.paymentRequest());
            }

            if (receivedTrade.hasPayoutRequest()) {
                trade.payoutRequest(receivedTrade.payoutRequest());
            }

            updatedTrade = walletManager.watchEscrowAddressAndResetBlockchain(trade.getEscrowAddress())
                    .map(ea -> receivedTrade.arbitrateRequest())
                    // create arbitrating trade from trade and arbitrate request
                    .map(trade::arbitrateRequest);
        }

        return updatedTrade;
    }

    @Override
    public Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade) {
        return Maybe.empty();
    }

    public Maybe<Trade> refundSeller(Trade trade) {

        // 1. sign and broadcast payout tx
        Maybe<String> refundTxHash = walletManager.refundEscrowToSeller(trade);

        // 2. confirm refund tx and create payout completed
        Maybe<PayoutCompleted> payoutCompleted = refundTxHash.map(ph -> new PayoutCompleted(ph, PayoutCompleted.Reason.ARBITRATOR_SELLER_REFUND));

        // 5. post payout completed
        return payoutCompleted.map(trade::payoutCompleted);
    }

    public Maybe<Trade> payoutBuyer(Trade trade) {

        // 1. sign and broadcast payout tx
        Maybe<String> payoutTxHash = walletManager.payoutEscrowToBuyer(trade);

        // 2. confirm refund tx and create payout completed
        Maybe<PayoutCompleted> payoutCompleted = payoutTxHash.map(ph -> new PayoutCompleted(ph, PayoutCompleted.Reason.ARBITRATOR_BUYER_PAYOUT));

        // 5. post payout completed
        return payoutCompleted.map(trade::payoutCompleted);
    }
}
