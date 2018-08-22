package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.trade.model.BuyRequest;
import com.bytabit.mobile.trade.model.PayoutRequest;
import com.bytabit.mobile.trade.model.Trade;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.bitcoinj.core.Address;

import java.math.BigDecimal;

import static com.bytabit.mobile.trade.model.ArbitrateRequest.Reason.NO_BTC;

public class BuyerProtocol extends TradeProtocol {

    public BuyerProtocol() {
        super();
    }

    // 1.B: create trade, post created trade
    public Single<Trade> createTrade(SellOffer sellOffer, BigDecimal buyBtcAmount) {

        return Single.zip(walletManager.getTradeWalletEscrowPubKey(),
                profileManager.loadOrCreateMyProfile().map(Profile::getPubKey),
                walletManager.getTradeWalletDepositAddress().map(Address::toBase58),
                (buyerEscrowPubKey, buyerProfilePubKey, buyerPayoutAddress) ->
                        Trade.builder()
                                .escrowAddress(walletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(), sellOffer.getSellerEscrowPubKey(), buyerEscrowPubKey))
                                .sellOffer(sellOffer)
                                .buyRequest(new BuyRequest(buyerEscrowPubKey, buyBtcAmount, buyerProfilePubKey, buyerPayoutAddress))
                                .build())
                .doOnSuccess(t -> walletManager.createEscrowWallet(t.getEscrowAddress()).subscribe().dispose());
    }

    // 1.B: create trade, post created trade
    @Override
    public Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade) {

        // TODO handle seller canceling created trade
        Maybe<Trade> updatedTrade = Maybe.empty();

        if (receivedTrade.hasPaymentRequest()) {
            trade.paymentRequest(receivedTrade.paymentRequest());
            updatedTrade = Maybe.just(trade);
        }
        return updatedTrade;
    }

    // 2.B: buyer receives payment request, confirm funding tx
    @Override
    public Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.empty();

        if (receivedTrade.hasArbitrateRequest()) {
            trade.arbitrateRequest(receivedTrade.arbitrateRequest());
            updatedTrade = Maybe.just(trade);
        }

        return updatedTrade;
    }

    // 3.B: buyer sends payment to seller and post payout request
    public Maybe<Trade> sendPayment(Trade trade, String paymentReference) {

        // 1. create payout request with buyer payout signature

        return walletManager.getPayoutSignature(trade)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .map(payoutSignature -> new PayoutRequest(paymentReference, payoutSignature))
                .map(trade::payoutRequest).toMaybe();
    }

    public void cancelTrade(Trade fundedTrade) {

//        if (fundedTrade.status().equals(FUNDED)) {
//
//            // 1. sign and broadcast refund tx
//            try {
//                String refundTxHash = walletManager.cancelEscrowToSeller(fundedTrade);
//
//                // 2. confirm refund tx and create payout completed
//                PayoutCompleted payoutCompleted = new PayoutCompleted(refundTxHash, BUYER_SELLER_REFUND);
//
////                try {
//                Trade canceledTrade = Trade.builder()
//                        .escrowAddress(fundedTrade.getEscrowAddress())
//                        .sellOffer(fundedTrade.sellOffer())
//                        .buyRequest(fundedTrade.buyRequest())
//                        .paymentRequest(fundedTrade.paymentRequest())
//                        .payoutCompleted(payoutCompleted)
//                        .build();
//
//                tradeService.put(canceledTrade.getEscrowAddress(), canceledTrade).subscribe();
//
////                } catch (IOException e) {
////                    log.error("Can't post payout completed to server.", e);
////                }
//
//            } catch (InsufficientMoneyException e) {
//                // TODO notify user
//                log.error("Insufficient funds to cancel escrow to seller.");
//            }
//        }
    }

    public void requestArbitrate(Trade currentTrade) {
        super.requestArbitrate(currentTrade, NO_BTC);
    }
}
