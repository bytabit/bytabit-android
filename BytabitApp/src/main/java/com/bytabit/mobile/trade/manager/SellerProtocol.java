package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.trade.model.PaymentRequest;
import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import io.reactivex.Maybe;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;

public class SellerProtocol extends TradeProtocol {

    public SellerProtocol() {
        super();
    }

    // 1.S: seller receives created trade with sell offer + buy request
    @Override
    Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade) {

        return Maybe.just(trade.copyBuilder().version(receivedTrade.getVersion()).build());

        // TODO handle buyer or seller canceling created trade
    }

    // 2.S: seller fund escrow and post payment request
    Maybe<Trade> fundEscrow(Trade trade) {

        // 0. watch escrow address
        Maybe<String> watchedEscrowAddress = walletManager.watchEscrowAddress(trade.getEscrowAddress());

        // 1. fund escrow
        Maybe<Transaction> fundingTx = watchedEscrowAddress
                .flatMap(ea -> walletManager.fundEscrow(ea, trade.getBtcAmount())).cache();

        // 2. create refund tx address and signature
        Maybe<Address> refundTxAddress = walletManager.getTradeWalletDepositAddress().cache();

        Maybe<PaymentDetails> paymentDetails = paymentDetailsManager.getLoadedPaymentDetails()
                .filter(pd -> pd.getCurrencyCode().equals(trade.getCurrencyCode()) && pd.getPaymentMethod().equals(trade.getPaymentMethod()))
                .singleElement().cache();

        Maybe<String> refundTxSignature = Maybe.zip(fundingTx, refundTxAddress, (ftx, ra) ->
                walletManager.getRefundSignature(trade, ftx, ra)).flatMap(rs -> rs);

        // 3. create payment request
        Maybe<PaymentRequest> paymentRequest = Maybe.zip(fundingTx, refundTxAddress, paymentDetails, refundTxSignature,
                (ftx, ra, pd, rs) -> new PaymentRequest(ftx.getHashAsString(), pd.getDetails(), ra.toBase58(), rs));

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

        if (receivedTrade.hasPayoutCompleted() && receivedTrade.getPayoutCompleted().getReason().equals(PayoutCompleted.Reason.BUYER_SELLER_REFUND)) {
            tradeBuilder.payoutCompleted(receivedTrade.getPayoutCompleted());
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
        Maybe<String> payoutTxHash = walletManager.payoutEscrowToBuyer(trade);

        // 2. confirm payout tx and create payout completed
        Maybe<PayoutCompleted> payoutCompleted = payoutTxHash.map(ph -> new PayoutCompleted(ph, PayoutCompleted.Reason.SELLER_BUYER_PAYOUT));

        // 5. post payout completed
        return payoutCompleted.map(pc -> trade.copyBuilder().payoutCompleted(pc).build().withStatus());
    }
}
