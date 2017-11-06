package com.bytabit.mobile.trade;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.trade.model.BuyRequest;
import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.PayoutRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import io.reactivex.Single;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static com.bytabit.mobile.trade.model.ArbitrateRequest.Reason.NO_BTC;
import static com.bytabit.mobile.trade.model.PayoutCompleted.Reason.BUYER_SELLER_REFUND;
import static com.bytabit.mobile.trade.model.Trade.Status.CREATED;
import static com.bytabit.mobile.trade.model.Trade.Status.FUNDED;

public class BuyerProtocol extends TradeProtocol {

    public BuyerProtocol() {
        super(LoggerFactory.getLogger(BuyerProtocol.class));
    }

    // 1.B: create trade, post created trade
    public Single<Trade> createTrade(SellOffer sellOffer, BigDecimal buyBtcAmount,
                                     String buyerEscrowPubKey, String buyerProfilePubKey,
                                     String buyerPayoutAddress) {

        // create buy request
        String tradeEscrowAddress = WalletManager.escrowAddress(sellOffer.getArbitratorProfilePubKey(),
                sellOffer.getSellerEscrowPubKey(), buyerEscrowPubKey);

        BuyRequest buyRequest = new BuyRequest(buyerEscrowPubKey, buyBtcAmount, buyerProfilePubKey, buyerPayoutAddress);

        // create trade
        Trade createdTrade = Trade.builder()
                .sellOffer(sellOffer)
                .escrowAddress(tradeEscrowAddress)
                .buyRequest(buyRequest)
                .build();

        // watch trade escrow address and buyerPayoutAddress (in case of arbitrated payout)
        walletManager.createEscrowWallet(createdTrade.getEscrowAddress());

        // post buy request to server
//        try {
        log.debug("Put buyRequest to create new trade: {}", createdTrade);

        return tradeService.put(createdTrade.getEscrowAddress(), createdTrade);
//        } catch (IOException ioe) {
//            log.error(ioe.getMessage());
//            throw new RuntimeException(ioe);
//        }
    }

    @Override
    public Trade handleCreated(Trade createdTrade) {
        return createdTrade;
    }

    // 2.B: buyer receives payment request, confirm funding tx, write payment request
    @Override
    public Trade handleFunded(Trade createdTrade, Trade fundedTrade) {

        Trade verifiedFundedTrade = null;
        if (createdTrade.status().equals(CREATED)) {
            TransactionWithAmt tx = walletManager.getEscrowTransactionWithAmt(fundedTrade.getEscrowAddress(), fundedTrade.getFundingTxHash());

            if (tx != null) {
                if (fundedTrade.getBtcAmount().add(walletManager.defaultTxFee()).compareTo(tx.getBtcAmt()) == 0) {
                    verifiedFundedTrade = fundedTrade;
                } else {
                    log.error("Trade not found for payment request or funding tx btc amount doesn't match buy offer btc amount.");
                }
            } else {
                log.error("Tx not found for payment request.");
            }
        }

        return verifiedFundedTrade;
    }

    // 3.B: buyer sends payment to seller and post payout request
    public void sendPayment(Trade fundedTrade, String paymentReference) {

        if (fundedTrade.status().equals(FUNDED)) {

            // 1. create payout request with buyer payout signature
            Transaction fundingTx = walletManager.getEscrowTransaction(fundedTrade.getEscrowAddress(), fundedTrade.getFundingTxHash());

            if (fundingTx != null) {
                String payoutSignature = walletManager.getPayoutSignature(fundedTrade, fundingTx);
                PayoutRequest payoutRequest = new PayoutRequest(paymentReference, payoutSignature);

                // 3. post payout request to server
//                try {
                Trade paidTrade = Trade.builder()
                        .escrowAddress(fundedTrade.getEscrowAddress())
                        .sellOffer(fundedTrade.sellOffer())
                        .buyRequest(fundedTrade.buyRequest())
                        .paymentRequest(fundedTrade.paymentRequest())
                        .payoutRequest(payoutRequest)
                        .build();

                tradeService.put(paidTrade.getEscrowAddress(), paidTrade).subscribe();

//                } catch (IOException e) {
//                    log.error("Can't put paid trade to server.");
//                }
            } else {
                log.error("Funding transaction not found for payout request.");
            }
        }
    }

    public void cancelTrade(Trade fundedTrade) {

        if (fundedTrade.status().equals(FUNDED)) {

            // 1. sign and broadcast refund tx
            try {
                String refundTxHash = walletManager.cancelEscrowToSeller(fundedTrade);

                // 2. confirm refund tx and create payout completed
                PayoutCompleted payoutCompleted = new PayoutCompleted(refundTxHash, BUYER_SELLER_REFUND);

//                try {
                Trade canceledTrade = Trade.builder()
                        .escrowAddress(fundedTrade.getEscrowAddress())
                        .sellOffer(fundedTrade.sellOffer())
                        .buyRequest(fundedTrade.buyRequest())
                        .paymentRequest(fundedTrade.paymentRequest())
                        .payoutCompleted(payoutCompleted)
                        .build();

                tradeService.put(canceledTrade.getEscrowAddress(), canceledTrade).subscribe();

//                } catch (IOException e) {
//                    log.error("Can't post payout completed to server.", e);
//                }

            } catch (InsufficientMoneyException e) {
                // TODO notify user
                log.error("Insufficient funds to cancel escrow to seller.");
            }
        }
    }

    public void requestArbitrate(Trade currentTrade) {
        super.requestArbitrate(currentTrade, NO_BTC);
    }
}
