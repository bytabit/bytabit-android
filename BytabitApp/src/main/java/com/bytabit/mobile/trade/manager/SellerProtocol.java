package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.trade.model.CancelCompleted;
import com.bytabit.mobile.trade.model.PaymentRequest;
import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import io.reactivex.Maybe;
import org.bitcoinj.core.Transaction;

public class SellerProtocol extends TradeProtocol {

    public SellerProtocol() {
        super();
    }

    // 1.S: seller receives created trade with sell offer + buy request
    @Override
    Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade) {

        Trade.TradeBuilder tradeBuilder = trade.copyBuilder().version(receivedTrade.getVersion());
        Maybe<Trade> updatedTrade = Maybe.just(tradeBuilder.build());

        if (receivedTrade.hasCancelCompleted()) {
            tradeBuilder.cancelCompleted(receivedTrade.getCancelCompleted());
            updatedTrade = Maybe.just(tradeBuilder.build());
        }

        return updatedTrade;
    }

    // 2.S: seller fund escrow and post payment request
    Maybe<Trade> fundEscrow(Trade trade) {

        // 0. watch escrow address
        Maybe<String> watchedEscrowAddress = walletManager.watchNewEscrowAddress(trade.getEscrowAddress());

        // 1. fund escrow
        Maybe<Transaction> fundingTx = watchedEscrowAddress
                .flatMap(ea -> walletManager.fundEscrow(ea, trade.getBtcAmount())).cache();

        // 2. create refund tx address and signature
        Maybe<String> refundAddressBase58 = walletManager.getDepositAddressBase58().cache();

        Maybe<PaymentDetails> paymentDetails = paymentDetailsManager.getLoadedPaymentDetails()
                .filter(pd -> pd.getCurrencyCode().equals(trade.getCurrencyCode()) && pd.getPaymentMethod().equals(trade.getPaymentMethod()))
                .singleElement().cache();

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

        if (receivedTrade.hasCancelCompleted() && receivedTrade.getCancelCompleted().getReason().equals(CancelCompleted.Reason.CANCEL_FUNDED)) {
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
