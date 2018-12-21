package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.offer.model.Offer;
import com.bytabit.mobile.trade.model.*;
import io.reactivex.Maybe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;

import static com.bytabit.mobile.offer.model.Offer.OfferType.BUY;
import static com.bytabit.mobile.offer.model.Offer.OfferType.SELL;
import static com.bytabit.mobile.trade.model.Trade.Role.ARBITRATOR;

public class BuyerProtocol extends TradeProtocol {

    public BuyerProtocol() {
        super();
    }

    // 1.B: create trade, post created trade
    Maybe<Trade> createTrade(Offer offer, BigDecimal buyBtcAmount) {
        if (!SELL.equals(offer.getOfferType())) {
            throw new TradeProtocolException("Buyer protocol can only create trade from sell offer.");
        }
        return Maybe.zip(walletManager.getEscrowPubKeyBase58(), walletManager.getProfilePubKeyBase58(),
                (takerEscrowPubKey, takerProfilePubKey) -> Trade.builder()
                        .role(Trade.Role.BUYER)
                        .status(Trade.Status.CREATED)
                        .createdTimestamp(ZonedDateTime.now())
                        .offer(offer)
                        .tradeRequest(TradeRequest.builder()
                                .takerProfilePubKey(takerProfilePubKey)
                                .takerEscrowPubKey(takerEscrowPubKey)
                                .btcAmount(buyBtcAmount)
                                .paymentAmount(buyBtcAmount.setScale(8, RoundingMode.UP)
                                        .multiply(offer.getPrice())
                                        .setScale(offer.getCurrencyCode().getScale(), RoundingMode.UP))
                                .build())
                        .build());
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

    // 2.B: buyer receives payment request, confirm funding tx
    @Override
    Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();
        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());

        if (receivedTrade.hasArbitrateRequest()) {
            tradeBuilder.arbitrateRequest(receivedTrade.getArbitrateRequest());
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
                trade.getFundingTransactionWithAmt().getTransaction(),
                trade.getArbitratorProfilePubKey(), trade.getSellerEscrowPubKey(), trade.getBuyerEscrowPubKey(),
                trade.getRefundAddress(), trade.getRefundTxSignature(), ARBITRATOR.equals(trade.getRole()));

        // 2. confirm refund tx and create cancel completed
        Maybe<CancelCompleted> cancelCompleted = refundTxHash.map(ph -> new CancelCompleted(ph, CancelCompleted.Reason.CANCEL_FUNDED));

        // 5. post cancel completed
        return cancelCompleted.map(pc -> trade.copyBuilder().cancelCompleted(pc).build().withStatus());
    }
}
