/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.app.core.trade.manager;


import com.bytabit.app.core.arbitrate.manager.ArbitratorManager;
import com.bytabit.app.core.common.HashUtils;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.trade.model.ArbitrateRequest;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.core.trade.model.TradeRequest;
import com.bytabit.app.core.wallet.manager.WalletManager;

import org.bitcoinj.core.Sha256Hash;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.UUID;

import io.reactivex.Maybe;
import io.reactivex.Single;
import lombok.NonNull;

abstract class TradeProtocol {

    WalletManager walletManager;

    ArbitratorManager arbitratorManager;

    TradeService tradeService;

    public TradeProtocol(WalletManager walletManager, ArbitratorManager arbitratorManager,
                         TradeService tradeService) {
        this.walletManager = walletManager;
        this.arbitratorManager = arbitratorManager;
        this.tradeService = tradeService;
    }

    // CREATED, ACCEPTED, *FUNDING*, FUNDED, PAID, *COMPLETING*, COMPLETED, ARBITRATING

    abstract Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade);

    Single<Trade> createTrade(Offer offer, Trade.Role role, BigDecimal btcAmount, String takerProfilePubKey, String takerEscrowPubKey) {

        BigDecimal paymentAmount = btcAmount.setScale(8, RoundingMode.UP)
                .multiply(offer.getPrice())
                .setScale(offer.getCurrencyCode().getScale(), RoundingMode.UP);

        TradeRequest tradeRequest = TradeRequest.builder()
                .takerProfilePubKey(takerProfilePubKey)
                .takerEscrowPubKey(takerEscrowPubKey)
                .btcAmount(btcAmount)
                .paymentAmount(paymentAmount)
                .build();

        Trade trade = Trade.builder()
                .id(getTradeId(offer, tradeRequest))
                .role(role)
                .status(Trade.Status.CREATED)
                .createdTimestamp(new Date())
                .offer(offer)
                .tradeRequest(tradeRequest)
                .build();

        return Single.just(trade);
    }

    Maybe<Trade> handleAccepted(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();
        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());

        if (receivedTrade.hasPaymentRequest()) {
            tradeBuilder.paymentRequest(receivedTrade.getPaymentRequest());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        if (receivedTrade.hasCancelCompleted()) {
            tradeBuilder.cancelCompleted(receivedTrade.getCancelCompleted());
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

    Single<Trade> requestArbitrate(Trade trade) {

        ArbitrateRequest.Reason reason;
        if (trade.getRole().equals(Trade.Role.SELLER)) {
            reason = ArbitrateRequest.Reason.NO_PAYMENT;
        } else if (trade.getRole().equals(Trade.Role.BUYER)) {
            reason = ArbitrateRequest.Reason.NO_BTC;
        } else {
            return Single.error(new TradeException("Invalid role, can't request arbitrate"));
        }

        ArbitrateRequest arbitrateRequest = ArbitrateRequest.builder().reason(reason).build();

        return Single.just(trade.copyBuilder().arbitrateRequest(arbitrateRequest).build());
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

    Maybe<Trade> handleCanceled(Trade trade, Trade receivedTrade) {

        return Maybe.empty();
    }

    public String getTradeId(@NonNull Offer offer, @NonNull TradeRequest tradeRequest) {

        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    protected Sha256Hash getTradeRequestSignedHash(@NonNull Offer offer, @NonNull TradeRequest tradeRequest) {
        return HashUtils.sha256Hash(offer.sha256Hash(), tradeRequest.sha256Hash(offer));
    }
}
