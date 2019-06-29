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
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.trade.model.CancelCompleted;
import com.bytabit.app.core.trade.model.PayoutRequest;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.core.trade.model.TradeAcceptance;
import com.bytabit.app.core.trade.model.TradeRequest;
import com.bytabit.app.core.wallet.manager.WalletManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Maybe;

import static com.bytabit.app.core.offer.model.Offer.OfferType.BUY;
import static com.bytabit.app.core.offer.model.Offer.OfferType.SELL;
import static com.bytabit.app.core.trade.model.Trade.Role.ARBITRATOR;

@Singleton
public class BuyerProtocol extends TradeProtocol {

    @Inject
    public BuyerProtocol(WalletManager walletManager, ArbitratorManager arbitratorManager,
                         TradeService tradeService) {
        super(walletManager, arbitratorManager, tradeService);
    }

    // 1.B: create trade, post created trade
    Maybe<Trade> createTrade(Offer offer, BigDecimal buyBtcAmount) {
        if (!SELL.equals(offer.getOfferType())) {
            return Maybe.error(new TradeException("Buyer protocol can only create trade from sell offer."));
        }

        BigDecimal currencyAmount = offer.getPrice().multiply(buyBtcAmount).setScale(offer.getCurrencyCode().getScale(), RoundingMode.HALF_UP);
        if (currencyAmount.compareTo(offer.getMinAmount()) < 0) {
            return Maybe.error(new TradeException(String.format("Trade amount can not be less than %s %s.", offer.getMinAmount(), offer.getCurrencyCode())));
        }
        if (currencyAmount.compareTo(offer.getMaxAmount()) > 0 || currencyAmount.compareTo(offer.getCurrencyCode().getMaxTradeAmount()) > 0) {
            return Maybe.error(new TradeException(String.format("Trade amount can not be more than %s %s.", offer.getMaxAmount(), offer.getCurrencyCode())));
        }
        return Maybe.zip(walletManager.getEscrowPubKeyBase58(), walletManager.getProfilePubKeyBase58(),
                (takerEscrowPubKey, takerProfilePubKey) -> {

                    TradeRequest tradeRequest = TradeRequest.builder()
                            .takerProfilePubKey(takerProfilePubKey)
                            .takerEscrowPubKey(takerEscrowPubKey)
                            .btcAmount(buyBtcAmount)
                            .paymentAmount(buyBtcAmount.setScale(8, RoundingMode.UP)
                                    .multiply(offer.getPrice())
                                    .setScale(offer.getCurrencyCode().getScale(), RoundingMode.UP))
                            .build();

                    String id = getId(offer, tradeRequest);

                    return Trade.builder()
                            .id(id)
                            .role(Trade.Role.BUYER)
                            .status(Trade.Status.CREATED)
                            .createdTimestamp(new Date())
                            .offer(offer)
                            .tradeRequest(tradeRequest)
                            .build();
                });
    }

    // 1.B: create trade, post created trade
    @Override
    Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();
        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());

        if (receivedTrade.hasCancelCompleted()) {
            tradeBuilder.cancelCompleted(receivedTrade.getCancelCompleted());
            updatedTrade = Maybe.just(tradeBuilder.build());
        } else if (SELL.equals(trade.getOffer().getOfferType()) && receivedTrade.hasConfirmation()) {
            tradeBuilder.tradeAcceptance(receivedTrade.getTradeAcceptance());
            updatedTrade = Maybe.just(tradeBuilder.build())
                    .flatMap(confirmedTrade -> walletManager.watchNewEscrowAddress(confirmedTrade.getTradeAcceptance().getEscrowAddress()).map(ea -> confirmedTrade));
        } else if (BUY.equals(trade.getOffer().getOfferType())) {
            // TODO in future confirm we have enough fiat
            updatedTrade = walletManager.getEscrowPubKeyBase58().map(escrowPubKey -> {
                String arbitratorPubKey = arbitratorManager.getArbitrator().getPubkey();
                String escrowAddress = walletManager.escrowAddress(arbitratorPubKey, trade.getTakerEscrowPubKey(), escrowPubKey);
                TradeAcceptance confirmation = TradeAcceptance.builder()
                        .arbitratorProfilePubKey(arbitratorPubKey)
                        .makerEscrowPubKey(escrowPubKey)
                        .escrowAddress(escrowAddress)
                        .build();
                tradeBuilder.tradeAcceptance(confirmation);
                return tradeBuilder.build();
            })
                    .flatMap(confirmedTrade -> walletManager.watchNewEscrowAddress(confirmedTrade.getTradeAcceptance().getEscrowAddress()).map(ea -> confirmedTrade))
                    .flatMapSingleElement(tradeService::put);
        }

        return updatedTrade;
    }

    Maybe<Trade> cancelUnfundedTrade(Trade trade) {

        // create cancel completed
        CancelCompleted cancelCompleted = CancelCompleted.builder().reason(CancelCompleted.Reason.BUYER_CANCEL_UNFUNDED).build();

        // post cancel completed
        return Maybe.just(trade.copyBuilder().cancelCompleted(cancelCompleted).build().withStatus());
    }

    // 2.B: buyer receives payment request, confirm funding tx
    @Override
    Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();
        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());

        if (receivedTrade.hasArbitrateRequest()) {
            tradeBuilder.arbitrateRequest(receivedTrade.getArbitrateRequest());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        if (receivedTrade.hasCancelCompleted() && receivedTrade.getCancelCompleted().getReason().equals(CancelCompleted.Reason.BUYER_CANCEL_FUNDED)) {
            tradeBuilder.cancelCompleted(receivedTrade.getCancelCompleted());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        return updatedTrade;
    }

    // 3.B: buyer sends payment to seller and post payout request
    Maybe<Trade> sendPayment(Trade trade, String paymentReference) {

        // 1. create payout request with buyer payout signature

        return walletManager.getDepositAddressBase58().flatMap(payoutAddress ->
                walletManager.getPayoutSignature(trade.getBtcAmount(), trade.getFundingTransactionWithAmt().getTransaction(),
                        trade.getArbitratorProfilePubKey(), trade.getSellerEscrowPubKey(), trade.getBuyerEscrowPubKey(),
                        payoutAddress).map(payoutSignature -> PayoutRequest.builder()
                        .paymentReference(paymentReference)
                        .payoutAddress(payoutAddress)
                        .payoutTxSignature(payoutSignature).build())
                        .map(pr -> trade.copyBuilder().payoutRequest(pr).build().withStatus()));
    }

    Maybe<Trade> cancelFundingTrade(Trade trade) {

        // 1. sign and broadcast refund tx
        Maybe<String> refundTxHash = walletManager.refundEscrowToSeller(trade.getBtcAmount(),
                trade.getTxFeePerKb(),
                trade.getFundingTransactionWithAmt().getTransaction(),
                trade.getArbitratorProfilePubKey(), trade.getSellerEscrowPubKey(), trade.getBuyerEscrowPubKey(),
                trade.getRefundAddress(), trade.getRefundTxSignature(), ARBITRATOR.equals(trade.getRole()));

        // 2. confirm refund tx and create cancel completed
        Maybe<CancelCompleted> cancelCompleted = refundTxHash.map(ph -> new CancelCompleted(ph, CancelCompleted.Reason.BUYER_CANCEL_FUNDED));

        // 5. post cancel completed
        return cancelCompleted.map(pc -> trade.copyBuilder().cancelCompleted(pc).build().withStatus());
    }

    Maybe<Trade> handleCanceled(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();

        if (trade.getCancelCompleted().getPayoutTxHash() == null
                && !trade.hasPayoutRequest()
                && receivedTrade.hasPaymentRequest()) {
            updatedTrade = cancelFundingTrade(trade);
        }

        return updatedTrade;
    }
}
