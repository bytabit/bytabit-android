package com.bytabit.mobile.trade;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.trade.evt.BuyerCreated;
import com.bytabit.mobile.trade.model.BuyRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.WalletManager;
import io.reactivex.Single;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static com.bytabit.mobile.trade.model.ArbitrateRequest.Reason.NO_BTC;

public class BuyerProtocol extends TradeProtocol {

    public BuyerProtocol() {
        super(LoggerFactory.getLogger(BuyerProtocol.class));
    }

    // 1.B: create trade, post created trade
    public Single<Trade> createTrade(SellOffer sellOffer, BigDecimal buyBtcAmount) {

        Single<Trade> createdTrade = Single.zip(profileManager.retrieveMyProfile(), walletManager.getFreshBase58AuthPubKey(), walletManager.getDepositAddress(),
                (buyerProfile, buyerEscrowPubKey, depositAddress) -> {
                    String buyerPayoutAddress = depositAddress.toBase58();

                    // TODO input validation
                    BuyRequest buyRequest = new BuyRequest(buyerEscrowPubKey, buyBtcAmount, buyerProfile.getPubKey(), buyerPayoutAddress);

                    // create escrow address
                    String tradeEscrowAddress = WalletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(),
                            sellOffer.getSellerEscrowPubKey(), buyerEscrowPubKey);

                    // create trade
                    return Trade.builder()
                            .sellOffer(sellOffer)
                            .escrowAddress(tradeEscrowAddress)
                            .buyRequest(buyRequest)
                            .build();
                });

        return createdTrade.flatMap(t -> tradeService.put(t.getEscrowAddress(), t));
    }

    // 1.B: create trade, post created trade
    @Override
    public Trade handleCreated(BuyerCreated createdTrade) {

        return Trade.builder()
                .sellOffer(createdTrade.getSellOffer())
                .escrowAddress(createdTrade.getEscrowAddress())
                .buyRequest(createdTrade.getBuyRequest())
                .build();
    }

    // 2.B: buyer receives payment request, confirm funding tx
    @Override
    public Trade handleFunded(Trade createdTrade, Trade fundedTrade) {

        Trade verifiedFundedTrade = null;
//        if (createdTrade.status().equals(CREATED)) {
//            TransactionWithAmt tx = walletManager.getEscrowTransactionWithAmt(fundedTrade.getEscrowAddress(), fundedTrade.getFundingTxHash());
//
//            if (tx != null) {
//                if (fundedTrade.getBtcAmount().add(walletManager.defaultTxFee()).compareTo(tx.getBtcAmt()) == 0) {
//                    verifiedFundedTrade = fundedTrade;
//                } else {
//                    log.error("Trade not found for payment request or funding tx btc amount doesn't match buy offer btc amount.");
//                }
//            } else {
//                log.error("Tx not found for payment request.");
//            }
//        }

        return verifiedFundedTrade;
    }

    // 3.B: buyer sends payment to seller and post payout request
    public void sendPayment(Trade fundedTrade, String paymentReference) {

//        if (fundedTrade.status().equals(FUNDED)) {

//            // 1. create payout request with buyer payout signature
//            Transaction fundingTx = walletManager.getEscrowTransaction(fundedTrade.getEscrowAddress(), fundedTrade.getFundingTxHash());
//
//            if (fundingTx != null) {
//                String payoutSignature = walletManager.getPayoutSignature(fundedTrade, fundingTx);
//                PayoutRequest payoutRequest = new PayoutRequest(paymentReference, payoutSignature);
//
//                // 3. post payout request to server
////                try {
//                Trade paidTrade = Trade.builder()
//                        .escrowAddress(fundedTrade.getEscrowAddress())
//                        .sellOffer(fundedTrade.sellOffer())
//                        .buyRequest(fundedTrade.buyRequest())
//                        .paymentRequest(fundedTrade.paymentRequest())
//                        .payoutRequest(payoutRequest)
//                        .build();
//
//                tradeService.put(paidTrade.getEscrowAddress(), paidTrade).subscribe();
//
////                } catch (IOException e) {
////                    log.error("Can't put paid trade to server.");
////                }
//            } else {
//                log.error("Funding transaction not found for payout request.");
//            }
//        }
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
