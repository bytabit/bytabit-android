package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.trade.model.TradeProtocolException;
import io.reactivex.Maybe;

import static com.bytabit.mobile.trade.model.Trade.Role.ARBITRATOR;
import static com.bytabit.mobile.trade.model.Trade.Role.SELLER;

public class ArbitratorProtocol extends TradeProtocol {

    public ArbitratorProtocol() {
        super();
    }

    @Override
    Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();

        if (receivedTrade.hasArbitrateRequest()) {

            Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion())
                    .arbitrateRequest(receivedTrade.getArbitrateRequest());

            if (receivedTrade.hasConfirmation()) {
                tradeBuilder.confirmation(receivedTrade.getConfirmation());
                walletManager.watchNewEscrowAddressAndResetBlockchain(trade.getConfirmation().getEscrowAddress());
            }

            if (receivedTrade.hasPaymentRequest()) {
                tradeBuilder.paymentRequest(receivedTrade.getPaymentRequest());
            }

            if (receivedTrade.hasPayoutRequest()) {
                tradeBuilder.payoutRequest(receivedTrade.getPayoutRequest());
            }

            updatedTrade = Maybe.just(tradeBuilder.build().withStatus());
        }

        return updatedTrade;
    }

    @Override
    Maybe<Trade> handleConfirmed(Trade trade, Trade receivedTrade) {
        return Maybe.empty();
    }

    @Override
    Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade) {
        return Maybe.empty();
    }

    Maybe<Trade> refundSeller(Trade trade) {

        // 1. sign and broadcast payout tx
        if (SELLER.equals(trade.getRole())) {
            throw new TradeProtocolException("Only arbitrator or buyer roles can refund escrow to seller.");
        }
        Maybe<String> refundTxHash = walletManager.refundEscrowToSeller(trade.getBtcAmount(),
                trade.getFundingTransactionWithAmt().getTransaction(),
                trade.getArbitratorProfilePubKey(),
                trade.getSellerEscrowPubKey(),
                trade.getBuyerEscrowPubKey(),
                trade.getRefundAddress(),
                trade.getRefundTxSignature(),
                ARBITRATOR.equals(trade.getRole()));

        // 2. confirm refund tx and create payout completed
        Maybe<PayoutCompleted> payoutCompleted = refundTxHash.map(ph -> new PayoutCompleted(ph, PayoutCompleted.Reason.ARBITRATOR_SELLER_REFUND));

        // 5. post payout completed
        return payoutCompleted.map(pc -> trade.copyBuilder().payoutCompleted(pc).build().withStatus());
    }

    Maybe<Trade> payoutBuyer(Trade trade) {

        // 1. sign and broadcast payout tx
        Maybe<String> payoutTxHash = walletManager.payoutEscrowToBuyer(trade.getBtcAmount(),
                trade.getFundingTransactionWithAmt().getTransaction(),
                trade.getArbitratorProfilePubKey(),
                trade.getSellerEscrowPubKey(),
                trade.getBuyerEscrowPubKey(),
                trade.getPayoutAddress(),
                trade.getPayoutTxSignature());

        // 2. confirm refund tx and create payout completed
        Maybe<PayoutCompleted> payoutCompleted = payoutTxHash.map(ph -> new PayoutCompleted(ph, PayoutCompleted.Reason.ARBITRATOR_BUYER_PAYOUT));

        // 5. post payout completed
        return payoutCompleted.map(pc -> trade.copyBuilder().payoutCompleted(pc).build().withStatus());
    }
}
