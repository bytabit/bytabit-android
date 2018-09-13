package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.profile.manager.PaymentDetailsManager;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.trade.model.ArbitrateRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.trade.model.TradeProtocolException;
import com.bytabit.mobile.wallet.manager.WalletManager;
import io.reactivex.Maybe;

import javax.inject.Inject;

abstract class TradeProtocol {

    @Inject
    WalletManager walletManager;

    @Inject
    ProfileManager profileManager;

    @Inject
    PaymentDetailsManager paymentDetailsManager;

    // CREATED, *FUNDING*, FUNDED, PAID, *COMPLETING*, COMPLETED, ARBITRATING

    abstract Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade);

    abstract Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade);

    Maybe<Trade> handlePaid(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();

        if (receivedTrade.hasArbitrateRequest()) {
            updatedTrade = Maybe.just(trade.copyBuilder()
                    .arbitrateRequest(receivedTrade.getArbitrateRequest())
                    .build());
        }

        if (receivedTrade.hasPayoutCompleted()) {
            updatedTrade = Maybe.just(trade.copyBuilder()
                    .payoutCompleted(receivedTrade.getPayoutCompleted())
                    .build());
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
                .map(t -> t.copyBuilder().arbitrateRequest(arbitrateRequest).build());
    }

    Maybe<Trade> handleArbitrating(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.just(trade);

        if (receivedTrade.hasPayoutCompleted()) {
            trade.copyBuilder().payoutCompleted(receivedTrade.getPayoutCompleted()).build();
            updatedTrade = Maybe.just(trade);
        }

        return updatedTrade;
    }
}
