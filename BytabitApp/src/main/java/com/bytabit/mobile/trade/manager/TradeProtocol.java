package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.arbitrate.manager.ArbitratorManager;
import com.bytabit.mobile.profile.manager.PaymentDetailsManager;
import com.bytabit.mobile.trade.model.ArbitrateRequest;
import com.bytabit.mobile.trade.model.CancelCompleted;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.trade.model.TradeProtocolException;
import com.bytabit.mobile.wallet.manager.WalletManager;
import io.reactivex.Maybe;

import javax.inject.Inject;

abstract class TradeProtocol {

    @Inject
    WalletManager walletManager;

    @Inject
    PaymentDetailsManager paymentDetailsManager;

    @Inject
    ArbitratorManager arbitratorManager;

    @Inject
    TradeService tradeService;

    // CREATED, *FUNDING*, FUNDED, PAID, *COMPLETING*, COMPLETED, ARBITRATING

    abstract Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade);

    Maybe<Trade> handleConfirmed(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();
        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());

        if (receivedTrade.hasPaymentRequest()) {
            tradeBuilder.paymentRequest(receivedTrade.getPaymentRequest());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        return updatedTrade;
    }

    Maybe<Trade> handleFunding(Trade trade) {

        Maybe<Trade> updatedTrade = Maybe.empty();

        if (trade.getFundingTransactionWithAmt() != null) {
            updatedTrade = Maybe.just(trade);
        }

        return updatedTrade;
    }

    abstract Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade);

    Maybe<Trade> handlePaid(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();
        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());

        if (receivedTrade.hasArbitrateRequest()) {
            tradeBuilder.arbitrateRequest(receivedTrade.getArbitrateRequest());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        if (receivedTrade.hasPayoutCompleted()) {
            tradeBuilder.payoutCompleted(receivedTrade.getPayoutCompleted());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        return updatedTrade;
    }

    Maybe<Trade> requestArbitrate(Trade trade) {

        ArbitrateRequest.Reason reason;
        if (trade.getRole().equals(Trade.Role.SELLER)) {
            reason = ArbitrateRequest.Reason.NO_PAYMENT;
        } else if (trade.getRole().equals(Trade.Role.BUYER)) {
            reason = ArbitrateRequest.Reason.NO_BTC;
        } else {
            throw new TradeProtocolException("Invalid role, can't request arbitrate");
        }

        ArbitrateRequest arbitrateRequest = new ArbitrateRequest(reason);

        return Maybe.just(trade)
                .map(t -> t.copyBuilder().arbitrateRequest(arbitrateRequest).build().withStatus());
    }

    Maybe<Trade> handleCompleting(Trade trade) {

        Maybe<Trade> updatedTrade = Maybe.empty();

        if (trade.getPayoutTransactionWithAmt() != null) {
            updatedTrade = Maybe.just(trade);
        }

        return updatedTrade;
    }

    Maybe<Trade> handleArbitrating(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();
        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());

        if (receivedTrade.hasPayoutCompleted()) {
            updatedTrade = Maybe.just(tradeBuilder.payoutCompleted(receivedTrade.getPayoutCompleted()).build());
        }

        return updatedTrade;
    }

    Maybe<Trade> cancelCreatedTrade(Trade trade) {

        // create cancel completed
        CancelCompleted cancelCompleted = CancelCompleted.builder().reason(CancelCompleted.Reason.CANCEL_CREATED).build();

        // post cancel completed
        return Maybe.just(trade.copyBuilder().cancelCompleted(cancelCompleted).build().withStatus());
    }
}
