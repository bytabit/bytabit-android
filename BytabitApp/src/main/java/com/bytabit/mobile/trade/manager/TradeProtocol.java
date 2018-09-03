package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.profile.manager.PaymentDetailsManager;
import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.trade.model.ArbitrateRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.manager.WalletManager;
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

    public Maybe<Trade> requestArbitrate(Trade trade) {

        ArbitrateRequest.Reason reason;
        if (trade.getRole().equals(Trade.Role.SELLER)) {
            reason = ArbitrateRequest.Reason.NO_PAYMENT;
        } else if (trade.getRole().equals(Trade.Role.BUYER)) {
            reason = ArbitrateRequest.Reason.NO_BTC;
        } else {
            throw new RuntimeException("Invalid role, can't request arbitrate");
        }

        ArbitrateRequest arbitrateRequest = new ArbitrateRequest(reason);

        return Maybe.just(trade)
                .map(t -> t.arbitrateRequest(arbitrateRequest));
    }

    public Maybe<Trade> handleArbitrating(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.just(trade);

        if (receivedTrade.hasPayoutCompleted()) {
            trade.payoutCompleted(receivedTrade.payoutCompleted());
            updatedTrade = Maybe.just(trade);
        }

        return updatedTrade;
    }
}
