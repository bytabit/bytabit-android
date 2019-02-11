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

package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.offer.model.Offer;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.trade.model.*;
import io.reactivex.Maybe;
import org.bitcoinj.core.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import static com.bytabit.mobile.offer.model.Offer.OfferType.BUY;
import static com.bytabit.mobile.offer.model.Offer.OfferType.SELL;

public class SellerProtocol extends TradeProtocol {

    public SellerProtocol() {
        super();
    }

    // 1.B: create trade, post created trade
    Maybe<Trade> createTrade(Offer offer, BigDecimal sellBtcAmount) {
        if (!BUY.equals(offer.getOfferType())) {
            throw new TradeProtocolException("Seller protocol can only create trade from buy offer.");
        }
        return Maybe.zip(walletManager.getEscrowPubKeyBase58(),
                walletManager.getProfilePubKeyBase58(),
                (takerEscrowPubKey, takerProfilePubKey) -> Trade.builder()
                        .role(Trade.Role.SELLER)
                        .status(Trade.Status.CREATED)
                        .createdTimestamp(new Date())
                        .offer(offer)
                        .tradeRequest(TradeRequest.builder()
                                .takerProfilePubKey(takerProfilePubKey)
                                .takerEscrowPubKey(takerEscrowPubKey)
                                .btcAmount(sellBtcAmount)
                                .paymentAmount(sellBtcAmount.setScale(8, RoundingMode.UP).multiply(offer.getPrice()).setScale(offer.getCurrencyCode().getScale(), RoundingMode.UP))
                                .build())
                        .build());
    }

    // 1.S: seller receives created trade with sell offer + buy request
    @Override
    Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade) {

        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());
        Maybe<Trade> updatedTrade = Maybe.just(tradeBuilder.build());

        if (receivedTrade.hasCancelCompleted()) {
            tradeBuilder.cancelCompleted(receivedTrade.getCancelCompleted());
            updatedTrade = Maybe.just(tradeBuilder.build());
        } else if (BUY.equals(trade.getOffer().getOfferType()) && receivedTrade.hasConfirmation()) {
            tradeBuilder.tradeAcceptance(receivedTrade.getTradeAcceptance());
            updatedTrade = Maybe.just(tradeBuilder.build())
                    .flatMap(confirmedTrade -> walletManager.watchNewEscrowAddress(confirmedTrade.getTradeAcceptance().getEscrowAddress()).map(ea -> confirmedTrade));
        } else if (SELL.equals(trade.getOffer().getOfferType())) {
            // confirm wallet has enough btc, if so update trade with tradeAcceptance
            updatedTrade = walletManager.getTradeWalletBalance()
                    .filter(walletBalance -> trade.getTradeRequest().getBtcAmount().compareTo(walletBalance) <= 0)
                    .flatMap(walletBalance -> walletManager.getEscrowPubKeyBase58().map(makerEscrowPubKey -> {
                        String arbitratorProfilePubKey = arbitratorManager.getArbitrator().getPubkey();
                        String escrowAddress = walletManager.escrowAddress(arbitratorProfilePubKey, makerEscrowPubKey, trade.getTakerEscrowPubKey());
                        TradeAcceptance confirmation = TradeAcceptance.builder()
                                .arbitratorProfilePubKey(arbitratorProfilePubKey)
                                .makerEscrowPubKey(makerEscrowPubKey)
                                .escrowAddress(escrowAddress)
                                .build();
                        return tradeBuilder.tradeAcceptance(confirmation).build();
                    }))
                    .flatMap(confirmedTrade -> walletManager.watchNewEscrowAddress(confirmedTrade.getTradeAcceptance().getEscrowAddress()).map(ea -> confirmedTrade))
                    .flatMapSingleElement(tradeService::put);
        }

        return updatedTrade;
    }

    Maybe<Trade> cancelUnfundedTrade(Trade trade) {

        // create cancel completed
        CancelCompleted cancelCompleted = CancelCompleted.builder().reason(CancelCompleted.Reason.SELLER_CANCEL_UNFUNDED).build();

        // post cancel completed
        return Maybe.just(trade.copyBuilder().cancelCompleted(cancelCompleted).build().withStatus());
    }

    // 2.S: seller fund escrow and post payment request
    Maybe<Trade> fundEscrow(Trade trade) {

        Maybe<PaymentDetails> paymentDetails = paymentDetailsManager.getLoadedPaymentDetails()
                .filter(pd -> pd.getCurrencyCode().equals(trade.getCurrencyCode()) && pd.getPaymentMethod().equals(trade.getPaymentMethod()))
                .singleElement().cache();

        // TODO don't fund escrow if payment details are not configured

        // 1. fund escrow
        Maybe<Transaction> fundingTx = walletManager.fundEscrow(trade.getTradeAcceptance().getEscrowAddress(), trade.getBtcAmount()).cache();

        // 2. create refund tx address and signature
        Maybe<String> refundAddressBase58 = walletManager.getDepositAddressBase58().cache();

        Maybe<String> refundTxSignature = Maybe.zip(fundingTx, refundAddressBase58, (ftx, refundAddress) ->
                walletManager.getPayoutSignature(trade.getBtcAmount(), ftx,
                        trade.getArbitratorProfilePubKey(),
                        trade.getSellerEscrowPubKey(),
                        trade.getBuyerEscrowPubKey(),
                        refundAddress))
                .flatMap(rs -> rs);

        // 3. create payment request
        Maybe<PaymentRequest> paymentRequest = Maybe.zip(fundingTx, refundAddressBase58, paymentDetails, refundTxSignature,
                (ftx, ra, pd, rs) -> new PaymentRequest(ftx.getHashAsString(), pd.getDetails(), ra, rs));

        return paymentRequest.map(pr -> trade.copyBuilder().paymentRequest(pr).build().withStatus());
    }

    @Override
    Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();

        Trade.TradeBuilder tradeBuilder = trade.copyBuilder()
                .version(receivedTrade.getVersion());

        if (receivedTrade.hasPayoutRequest()) {
            tradeBuilder.payoutRequest(receivedTrade.getPayoutRequest());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        if (receivedTrade.hasCancelCompleted() && receivedTrade.getCancelCompleted().getReason().equals(CancelCompleted.Reason.BUYER_CANCEL_FUNDED)) {
            tradeBuilder.cancelCompleted(receivedTrade.getCancelCompleted());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        if (receivedTrade.hasArbitrateRequest()) {
            tradeBuilder.arbitrateRequest(receivedTrade.getArbitrateRequest());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        return updatedTrade;
    }

    // 3.S: seller payout escrow to buyer and write payout details
    Maybe<Trade> confirmPaymentReceived(Trade trade) {

        // 1. sign and broadcast payout tx
        Maybe<String> payoutTxHash = walletManager.payoutEscrowToBuyer(trade.getBtcAmount(),
                trade.getFundingTransactionWithAmt().getTransaction(),
                trade.getArbitratorProfilePubKey(),
                trade.getSellerEscrowPubKey(),
                trade.getBuyerEscrowPubKey(),
                trade.getPayoutAddress(),
                trade.getPayoutTxSignature());

        // 2. confirm payout tx and create payout completed
        Maybe<PayoutCompleted> payoutCompleted = payoutTxHash.map(ph -> new PayoutCompleted(ph, PayoutCompleted.Reason.SELLER_BUYER_PAYOUT));

        // 5. post payout completed
        return payoutCompleted.map(pc -> trade.copyBuilder().payoutCompleted(pc).build().withStatus());
    }
}
