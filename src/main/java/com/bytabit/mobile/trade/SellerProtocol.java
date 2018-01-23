package com.bytabit.mobile.trade;

import com.bytabit.mobile.profile.manager.ProfileManager;
import com.bytabit.mobile.trade.evt.BuyerCreated;
import com.bytabit.mobile.trade.model.PaymentRequest;
import com.bytabit.mobile.trade.model.Trade;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.bytabit.mobile.trade.model.ArbitrateRequest.Reason.NO_PAYMENT;

public class SellerProtocol extends TradeProtocol {

    @Inject
    private ProfileManager profileManager;

    public SellerProtocol() {
        super(LoggerFactory.getLogger(SellerProtocol.class));
    }

    // 1.S: seller receives created trade with sell offer + buy request
    //@Override
    public Trade handleCreated(Trade createdTrade) {

        Trade fundedTrade = null;

        // watch trade escrow address
//        walletManager.createEscrowWallet(createdTrade.getEscrowAddress());

        // fund escrow and create paymentRequest
        PaymentRequest paymentRequest = fundEscrow(createdTrade);

        if (paymentRequest != null) {

            // 4. put payment request
//            try {
                fundedTrade = Trade.builder()
                        .escrowAddress(createdTrade.getEscrowAddress())
                        .sellOffer(createdTrade.sellOffer())
                        .buyRequest(createdTrade.buyRequest())
                        .paymentRequest(paymentRequest)
                        .build();

            tradeService.put(fundedTrade.getEscrowAddress(), fundedTrade).subscribe();

//            } catch (IOException e) {
//                log.error("Unable to PUT funded trade.", e);
//                // TODO retry putting payment request
//            }
        } else {
            log.error("Unable to fund trade.");
        }

        return fundedTrade;
    }

    // 2.S: seller fund escrow and post payment request
    private PaymentRequest fundEscrow(Trade trade) {

        // TODO verify escrow not yet funded ?
//        try {
//            // 1. fund escrow
//            Transaction fundingTx = walletManager.fundEscrow(trade.getEscrowAddress(),
//                    trade.getBtcAmount());
//
//            // 2. create refund tx address and signature
//
//            Address refundTxAddress = walletManager.getDepositAddress();
//            String refundTxSignature = walletManager.getRefundSignature(trade, fundingTx, refundTxAddress);
//
//            // 3. create payment request
//            String paymentDetails = profileManager.getPaymentDetails(
//                    trade.getCurrencyCode(),
//                    trade.getPaymentMethod()).blockingGet();
//
//            return new PaymentRequest(fundingTx.getHashAsString(), paymentDetails, refundTxAddress.toBase58(), refundTxSignature);
//
//        } catch (InsufficientMoneyException e) {
//            log.error("Insufficient BTC to fund trade escrow.");
//            // TODO let user know not enough BTC in wallet
//            return null;
//        }
        return null;
    }

    @Override
    public Trade handleCreated(BuyerCreated created) {
        return null;
    }

    @Override
    public Trade handleFunded(Trade createdTrade, Trade fundedTrade) {
        return null;
    }

    // 3.S: seller payout escrow to buyer and write payout details
    public Trade confirmPaymentReceived(Trade paidTrade) {

        Trade completedTrade = null;

//        if (paidTrade.status().equals(PAID)) {
//
//            // 1. sign and broadcast payout tx
//            try {
//                String payoutTxHash = walletManager.payoutEscrowToBuyer(paidTrade);
//
//                // 2. confirm payout tx and create payout completed
//                PayoutCompleted payoutCompleted = new PayoutCompleted(payoutTxHash, PayoutCompleted.Reason.SELLER_BUYER_PAYOUT);
//
//                // 5. post payout completed
////                try {
//
//                    completedTrade = Trade.builder()
//                            .escrowAddress(paidTrade.getEscrowAddress())
//                            .sellOffer(paidTrade.sellOffer())
//                            .buyRequest(paidTrade.buyRequest())
//                            .paymentRequest(paidTrade.paymentRequest())
//                            .payoutRequest(paidTrade.payoutRequest())
//                            .payoutCompleted(payoutCompleted)
//                            .build();
//
//                tradeService.put(completedTrade.getEscrowAddress(), completedTrade).subscribe();
//
////                } catch (IOException e) {
////                    log.error("Can't post payout completed to server.");
////                }
//
//            } catch (InsufficientMoneyException e) {
//                // TODO notify user
//                log.error("Insufficient funds to payout escrow to buyer.");
//            }
//        }

        return completedTrade;
    }

    public void requestArbitrate(Trade currentTrade) {
        super.requestArbitrate(currentTrade, NO_PAYMENT);
    }
}
