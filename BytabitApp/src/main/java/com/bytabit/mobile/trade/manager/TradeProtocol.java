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
                    .filter(t -> transactionWithAmt.getTransactionBigDecimalAmt().negate().subtract(walletManager.defaultTxFee()).compareTo(t.getBtcAmount()) == 0)
                    .filter(t -> transactionWithAmt.getOutputAddress().equals(t.getBuyerPayoutAddress()) || transactionWithAmt.getOutputAddress().equals(t.getRefundAddress()))
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

    protected void requestArbitrate(Trade trade, ArbitrateRequest.Reason reason) {

        ArbitrateRequest arbitrateRequest = new ArbitrateRequest(reason);

        Trade arbitratingTrade = Trade.builder()
                .escrowAddress(trade.getEscrowAddress())
                .sellOffer(trade.sellOffer())
                .buyRequest(trade.buyRequest())
                .paymentRequest(trade.paymentRequest())
                .payoutRequest(trade.payoutRequest())
                .arbitrateRequest(arbitrateRequest)
                .build();
    }

    public Maybe<Trade> handleArbitrating(Trade trade, Trade receivedTrade) {

        return Maybe.just(trade);
    }
}
