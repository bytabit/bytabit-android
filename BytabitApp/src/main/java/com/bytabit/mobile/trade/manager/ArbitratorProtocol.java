package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import io.reactivex.Maybe;

public class ArbitratorProtocol extends TradeProtocol {

    public ArbitratorProtocol() {
        super();
    }

    @Override
    Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();
        Trade.TradeBuilder tradeBuilder = trade.copyBuilder();

        if (receivedTrade.hasArbitrateRequest()) {

            if (receivedTrade.hasPaymentRequest()) {
                tradeBuilder.paymentRequest(receivedTrade.getPaymentRequest());
            }

            if (receivedTrade.hasPayoutRequest()) {
                tradeBuilder.payoutRequest(receivedTrade.getPayoutRequest());
            }

            updatedTrade = walletManager.watchEscrowAddressAndResetBlockchain(trade.getEscrowAddress())
                    .map(ea -> receivedTrade.getArbitrateRequest())
                    // create arbitrating trade from trade and arbitrate request
                    .map(ar -> tradeBuilder.arbitrateRequest(ar).build());
        }

        return updatedTrade;
    }

    @Override
    Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade) {
        return Maybe.empty();
    }

    Maybe<Trade> refundSeller(Trade trade) {

        // 1. sign and broadcast payout tx
        Maybe<String> refundTxHash = walletManager.refundEscrowToSeller(trade);

        // 2. confirm refund tx and create payout completed
        Maybe<PayoutCompleted> payoutCompleted = refundTxHash.map(ph -> new PayoutCompleted(ph, PayoutCompleted.Reason.ARBITRATOR_SELLER_REFUND));

        // 5. post payout completed
        return payoutCompleted.map(pc -> trade.copyBuilder().payoutCompleted(pc).build());
    }

    Maybe<Trade> payoutBuyer(Trade trade) {

        // 1. sign and broadcast payout tx
        Maybe<String> payoutTxHash = walletManager.payoutEscrowToBuyer(trade);

        // 2. confirm refund tx and create payout completed
        Maybe<PayoutCompleted> payoutCompleted = payoutTxHash.map(ph -> new PayoutCompleted(ph, PayoutCompleted.Reason.ARBITRATOR_BUYER_PAYOUT));

        // 5. post payout completed
        return payoutCompleted.map(pc -> trade.copyBuilder().payoutCompleted(pc).build());
    }
}
