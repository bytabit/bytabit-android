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

package com.bytabit.app.core.trade;

import com.bytabit.app.core.arbitrate.ArbitratorManager;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.payment.PaymentDetailsManager;
import com.bytabit.app.core.payment.model.PaymentDetails;
import com.bytabit.app.core.trade.model.CancelCompleted;
import com.bytabit.app.core.trade.model.PaymentRequest;
import com.bytabit.app.core.trade.model.PayoutCompleted;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.core.trade.model.TradeAcceptance;
import com.bytabit.app.core.wallet.WalletManager;

import org.bitcoinj.core.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Maybe;
import io.reactivex.Single;

import static com.bytabit.app.core.offer.model.Offer.OfferType.BUY;
import static com.bytabit.app.core.offer.model.Offer.OfferType.SELL;
import static com.bytabit.app.core.trade.model.Trade.Role.SELLER;

@Singleton
public class SellerProtocol extends TradeProtocol {

    private final PaymentDetailsManager paymentDetailsManager;

    @Inject
    public SellerProtocol(WalletManager walletManager, ArbitratorManager arbitratorManager,
                          TradeService tradeService, PaymentDetailsManager paymentDetailsManager) {
        super(walletManager, arbitratorManager, tradeService);
        this.paymentDetailsManager = paymentDetailsManager;
    }

    // 1.B: create trade, post created trade
    Maybe<Trade> createTrade(Offer offer, BigDecimal sellBtcAmount) {
        if (!BUY.equals(offer.getOfferType())) {
            return Maybe.error(new TradeException("Seller protocol can only create trade from buy offer."));
        }

        BigDecimal currencyAmount = offer.getPrice().multiply(sellBtcAmount).setScale(offer.getCurrencyCode().getScale(), RoundingMode.HALF_UP);
        if (currencyAmount.compareTo(offer.getMinAmount()) < 0) {
            return Maybe.error(new TradeException(String.format("Trade amount can not be less than %s %s.", offer.getMinAmount(), offer.getCurrencyCode())));
        }
        if (currencyAmount.compareTo(offer.getMaxAmount()) > 0 || currencyAmount.compareTo(offer.getCurrencyCode().getMaxTradeAmount()) > 0) {
            return Maybe.error(new TradeException(String.format("Trade amount can not be more than %s %s.", offer.getMaxAmount(), offer.getCurrencyCode())));
        }

        return Maybe.zip(walletManager.getEscrowPubKeyBase58(), walletManager.getProfilePubKeyBase58(),
                (takerEscrowPubKey, takerProfilePubKey) -> createTrade(offer, SELLER, sellBtcAmount, takerProfilePubKey, takerEscrowPubKey))
                .flatMapSingleElement(t -> t);
    }

    // 1.S: seller receives created trade with sell offer + buy request
    @Override
    Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade) {

        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());
        Maybe<Trade> updatedTrade = Maybe.just(tradeBuilder.build());

        if (receivedTrade.hasCancelCompleted()) {
            tradeBuilder.cancelCompleted(receivedTrade.getCancelCompleted());
            updatedTrade = Maybe.just(tradeBuilder.build());
        } else if (BUY.equals(trade.getOffer().getOfferType()) && receivedTrade.hasAcceptance()) {
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

    Single<Trade> cancelUnfundedTrade(Trade trade) {

        // create cancel completed
        CancelCompleted cancelCompleted = CancelCompleted.builder().reason(CancelCompleted.Reason.SELLER_CANCEL_UNFUNDED).build();

        // post cancel completed
        return Single.just(trade.copyBuilder().cancelCompleted(cancelCompleted).build());
    }

    // 2.S: seller fund escrow and post payment request
    Single<Trade> fundEscrow(Trade trade) {

        Single<PaymentDetails> paymentDetails = paymentDetailsManager.getLoadedPaymentDetails()
                .filter(pd -> pd.getCurrencyCode().equals(trade.getCurrencyCode()) && pd.getPaymentMethod().equals(trade.getPaymentMethod()))
                .singleOrError()
                .onErrorResumeNext(Single.error(new TradeException("No payment details found to fund trade.")))
                .cache();

        // 1. fund escrow
        BigDecimal txFeePerKb = walletManager.defaultTxFee();
        Maybe<Transaction> fundingTx = walletManager.fundEscrow(trade.getTradeAcceptance().getEscrowAddress(), trade.getBtcAmount(), txFeePerKb).cache();

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
        Single<PaymentRequest> paymentRequest = Maybe.zip(paymentDetails.toMaybe(), refundAddressBase58, refundTxSignature, fundingTx,
                (pd, ra, rs, ftx) -> PaymentRequest.builder()
                        .fundingTxHash(ftx.getHashAsString())
                        .paymentDetails(pd.getDetails())
                        .refundAddress(ra)
                        .refundTxSignature(rs)
                        .txFeePerKb(txFeePerKb)
                        .build()).toSingle();

        return paymentRequest.map(pr -> trade.copyBuilder().paymentRequest(pr).build());
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
    Single<Trade> confirmPaymentReceived(Trade trade) {

        // 1. sign and broadcast payout tx
        Single<String> payoutTxHash = walletManager.payoutEscrowToBuyer(trade.getBtcAmount(),
                trade.getTxFeePerKb(),
                trade.getFundingTransactionWithAmt().getTransaction(),
                trade.getArbitratorProfilePubKey(),
                trade.getSellerEscrowPubKey(),
                trade.getBuyerEscrowPubKey(),
                trade.getPayoutAddress(),
                trade.getPayoutTxSignature()).toSingle();

        // 2. confirm payout tx and create payout completed
        Single<PayoutCompleted> payoutCompleted = payoutTxHash.map(ph -> PayoutCompleted.builder()
                .payoutTxHash(ph)
                .reason(PayoutCompleted.Reason.SELLER_BUYER_PAYOUT)
                .build());

        // 5. post payout completed
        return payoutCompleted.map(pc -> trade.copyBuilder().payoutCompleted(pc).build());
    }
}
