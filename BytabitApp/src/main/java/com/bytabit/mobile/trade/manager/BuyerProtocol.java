package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.offer.model.Offer;
import com.bytabit.mobile.trade.model.CancelCompleted;
import com.bytabit.mobile.trade.model.PayoutRequest;
import com.bytabit.mobile.trade.model.TakeOfferRequest;
import com.bytabit.mobile.trade.model.Trade;
import io.reactivex.Maybe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;

import static com.bytabit.mobile.trade.model.Trade.Role.ARBITRATOR;

public class BuyerProtocol extends TradeProtocol {

    public BuyerProtocol() {
        super();
    }

    // 1.B: create trade, post created trade
    Maybe<Trade> createTrade(Offer offer, BigDecimal buyBtcAmount) {

        return Maybe.zip(walletManager.getEscrowPubKeyBase58(),
                walletManager.getProfilePubKeyBase58(),
                walletManager.getDepositAddressBase58(),
                (takerEscrowPubKey, takerProfilePubKey, takerPayoutAddress) ->
                        Trade.builder()
                                .role(Trade.Role.BUYER)
                                .status(Trade.Status.CREATED)
                                //.escrowAddress(walletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(), sellOffer.getTraderEscrowPubKey(), buyerEscrowPubKey))
                                .createdTimestamp(ZonedDateTime.now())
                                .offer(offer)
                                .takeOfferRequest(TakeOfferRequest.builder()
                                        .takerProfilePubKey(takerProfilePubKey)
                                        .takerEscrowPubKey(takerEscrowPubKey)
                                        .btcAmount(buyBtcAmount)
                                        .paymentAmount(buyBtcAmount.setScale(8, RoundingMode.UP).multiply(offer.getPrice()).setScale(offer.getCurrencyCode().getScale(), RoundingMode.UP))
                                        .build())
                                .build())
                .flatMap(t -> walletManager.watchNewEscrowAddress(t.getEscrowAddress()).map(e -> t.withStatus()));
    }

    // 1.B: create trade, post created trade
    @Override
    Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();
        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());

        if (receivedTrade.hasPaymentRequest()) {
            tradeBuilder.paymentRequest(receivedTrade.getPaymentRequest());
            updatedTrade = Maybe.just(tradeBuilder.build());
        } else if (receivedTrade.hasCancelCompleted()) {
            tradeBuilder.cancelCompleted(receivedTrade.getCancelCompleted());
            updatedTrade = Maybe.just(tradeBuilder.build());
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

        return walletManager.getPayoutSignature(trade.getBtcAmount(), trade.getFundingTransactionWithAmt().getTransaction(),
                trade.getArbitratorProfilePubKey(), trade.getSellerEscrowPubKey(), trade.getBuyerEscrowPubKey(),
                trade.getPayoutAddress()).zipWith(walletManager.getDepositAddressBase58(),
                (payoutSignature, payoutAddress) -> PayoutRequest.builder()
                        .paymentReference(paymentReference)
                        .payoutAddress(payoutAddress)
                        .payoutTxSignature(payoutSignature).build())
                .map(pr -> trade.copyBuilder().payoutRequest(pr).build().withStatus());
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
