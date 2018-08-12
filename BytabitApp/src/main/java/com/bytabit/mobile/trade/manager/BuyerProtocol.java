package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.trade.model.BuyRequest;
import com.bytabit.mobile.trade.model.PayoutRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.bitcoinj.core.Address;

import java.math.BigDecimal;

import static com.bytabit.mobile.trade.model.ArbitrateRequest.Reason.NO_BTC;
import static com.bytabit.mobile.trade.model.Trade.Status.CREATED;

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
                //.flatMap(this::writeTrade)
                .flatMap(walletManager::createOrLoadEscrowWallet);
        //.flatMap(t -> tradeService.put(t).toObservable());

//        Single<Trade> createdTrade = Single.zip(profileManager.loadOrCreateMyProfile(), walletManager.getFreshBase58AuthPubKey(), walletManager.getDepositAddress(),
//                (buyerProfile, buyerEscrowPubKey, depositAddress) -> {
//                    String buyerPayoutAddress = depositAddress.toBase58();
//
//                    // TODO input validation
//                    BuyRequest buyRequest = new BuyRequest(buyerEscrowPubKey, buyBtcAmount, buyerProfile.getPubKey(), buyerPayoutAddress);
//
//                    // create escrow address
//                    String tradeEscrowAddress = WalletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(),
//                            sellOffer.getSellerEscrowPubKey(), buyerEscrowPubKey);
//
//                    // create trade
//                    return Trade.builder()
//                            .sellOffer(sellOffer)
//                            .escrowAddress(tradeEscrowAddress)
//                            .buyRequest(buyRequest)
//                            .build();
//                });

//        return createdTrade.flatMap(t -> tradeService.put(t.getEscrowAddress(), t));
//        return null;
    }

    // 1.B: create trade, post created trade
    @Override
    public Maybe<Trade> handleCreated(Trade currentTrade, Trade createdTrade) {

        return Maybe.just(createdTrade);
    }

    // 2.B: buyer receives payment request, confirm funding tx
    @Override
    public Maybe<Trade> handleFunding(Trade currentTrade, Trade fundingTrade) {

        //Maybe<Trade> createdTrade = readTrade(fundedTrade.getEscrowAddress());

        Maybe<Trade> verifiedFundingTrade = Maybe.empty();

        if (currentTrade.status().equals(CREATED)) {
            Maybe<TransactionWithAmt> tx = walletManager.getEscrowTransactionWithAmt(fundingTrade.getEscrowAddress(), fundingTrade.getFundingTxHash());

            // TODO validate all details match currentTrade
            // TODO update currentTrade with payment details from received trade
            verifiedFundingTrade = Maybe.just(fundingTrade);
        }

        return verifiedFundingTrade;
    }

    // 3.B: buyer sends payment to seller and post payout request
    public Maybe<Trade> sendPayment(Trade fundedTrade, String paymentReference) {

        Maybe<Trade> paidTrade = Maybe.empty();

        if (Trade.Status.FUNDED.equals(fundedTrade.status())) {

            // 1. create payout request with buyer payout signature
            //Transaction fundingTx = fundedTrade.fundingTransactionWithAmt().

            if (fundedTrade.fundingTransactionWithAmt() != null) {
                paidTrade = walletManager.getPayoutSignature(fundedTrade)
                        .observeOn(Schedulers.io())
                        .subscribeOn(Schedulers.io())
                        .map(payoutSignature -> {
                            PayoutRequest payoutRequest = new PayoutRequest(paymentReference, payoutSignature);

                            // 3. post payout request to server
//                try {
                            return Trade.builder()
                                    .escrowAddress(fundedTrade.getEscrowAddress())
                                    .sellOffer(fundedTrade.sellOffer())
                                    .buyRequest(fundedTrade.buyRequest())
                                    .paymentRequest(fundedTrade.paymentRequest())
                                    .payoutRequest(payoutRequest)
                                    .build();
                        }).toMaybe();

                //tradeService.put(paidTrade.getEscrowAddress(), paidTrade).subscribe();

//                } catch (IOException e) {
//                    log.error("Can't put paid trade to server.");
//                }
            } else {
                //log.error("Funding transaction not found for payout request.");
                //return Observable.empty();
            }

        }

        return paidTrade;
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
