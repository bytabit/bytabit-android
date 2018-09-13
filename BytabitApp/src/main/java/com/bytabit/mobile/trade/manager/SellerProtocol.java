package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.Profile;
import com.bytabit.mobile.trade.model.PaymentRequest;
import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;

public class SellerProtocol extends TradeProtocol {

    public SellerProtocol() {
        super();
    }

    // 1.S: seller receives created trade with sell offer + buy request
    @Override
    Maybe<Trade> handleCreated(Trade trade, Trade receivedTrade) {

        return walletManager.watchEscrowAddress(trade.getEscrowAddress())
                // fund escrow and create paymentRequest
                .flatMap(ea -> fundEscrow(trade))
                // create funded trade from created trade and payment request
                .map(pr -> trade.copyBuilder().paymentRequest(pr).build());
    }

    // 2.S: seller fund escrow and post payment request
    private Maybe<PaymentRequest> fundEscrow(Trade trade) {

        // 1. fund escrow
        Maybe<Transaction> fundingTx = walletManager.fundEscrow(trade.getEscrowAddress(), trade.getBtcAmount());

        // 2. create refund tx address and signature

        Single<Address> refundTxAddress = walletManager.getTradeWalletDepositAddress().toSingle();

        Single<Profile> profile = profileManager.loadOrCreateMyProfile();

        Single<PaymentDetails> paymentDetails = paymentDetailsManager.getLoadedPaymentDetails()
                .filter(pd -> pd.getCurrencyCode().equals(trade.getCurrencyCode()) && pd.getPaymentMethod().equals(trade.getPaymentMethod()))
                .singleOrError();

        return Maybe.zip(fundingTx, refundTxAddress.toMaybe(), profile.toMaybe(), (ftx, ra, p) -> {

            Single<String> refundTxSignature = walletManager.getRefundSignature(trade, ftx, ra).toSingle();

            // 3. create payment request
            return Single.zip(refundTxSignature, paymentDetails, (rs, pd) ->
                    new PaymentRequest(ftx.getHashAsString(), pd.getDetails(), ra.toBase58(), rs));

        }).flatMap(Single::toMaybe);
    }

    @Override
    Maybe<Trade> handleFunded(Trade trade, Trade receivedTrade) {

        Maybe<Trade> updatedTrade = Maybe.just(trade);

        if (receivedTrade.hasPayoutRequest()) {
            updatedTrade = Maybe.just(trade.copyBuilder().payoutRequest(receivedTrade.getPayoutRequest()).build());
        }

        if (receivedTrade.hasPayoutCompleted() && receivedTrade.getPayoutCompleted().getReason().equals(PayoutCompleted.Reason.BUYER_SELLER_REFUND)) {
            updatedTrade = Maybe.just(trade.copyBuilder().payoutCompleted(receivedTrade.getPayoutCompleted()).build());
        }

        if (receivedTrade.hasArbitrateRequest()) {
            updatedTrade = Maybe.just(trade.copyBuilder().arbitrateRequest(receivedTrade.getArbitrateRequest()).build());
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
        return payoutCompleted.map(pc -> trade.copyBuilder().payoutCompleted(pc).build());
    }
}
