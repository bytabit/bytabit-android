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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.bytabit.mobile.trade.model.ArbitrateRequest.Reason.NO_PAYMENT;
import static com.bytabit.mobile.trade.model.Trade.Status.PAID;

public class SellerProtocol extends TradeProtocol {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public SellerProtocol() {
        super();
    }

    // 1.S: seller receives created trade with sell offer + buy request
    @Override
    public Maybe<Trade> handleCreated(Trade currentTrade, Trade createdTrade) {

        //Maybe<Trade> currentTrade = readTrade(createdTrade.getEscrowAddress());

        // TODO verify no current trade for this escrowAddress
        return walletManager.createEscrowWallet(createdTrade.getEscrowAddress())
                // fund escrow and create paymentRequest
                .flatMap(ea -> fundEscrow(createdTrade))
                // create funded trade from created trade and payment request
                .map(pr -> Trade.builder()
                        .escrowAddress(createdTrade.getEscrowAddress())
                        .sellOffer(createdTrade.sellOffer())
                        .buyRequest(createdTrade.buyRequest())
                        .paymentRequest(pr)
                        .build()
                );

        // store funded trade
        //.flatMap(this::writeTrade)
        // put funded trade
        //.flatMap(ft -> tradeService.put(ft).toObservable());
    }

    // 2.S: seller fund escrow and post payment request
    private Maybe<PaymentRequest> fundEscrow(Trade trade) {

        // TODO verify escrow not yet funded ?

        // 1. fund escrow
        Single<Transaction> fundingTx = walletManager.fundEscrow(trade.getEscrowAddress(),
                trade.getBtcAmount());

        // 2. create refund tx address and signature

        Single<Address> refundTxAddress = walletManager.getTradeWalletDepositAddress();

        Single<Profile> profile = profileManager.loadOrCreateMyProfile();

        Single<PaymentDetails> paymentDetails = paymentDetailsManager.getLoadedPaymentDetails()
                .filter(pd -> pd.getCurrencyCode().equals(trade.getCurrencyCode()) && pd.getPaymentMethod().equals(trade.getPaymentMethod()))
                .singleOrError();

        return Single.zip(fundingTx, refundTxAddress, profile, (ftx, ra, p) -> {

            Single<String> refundTxSignature = walletManager.getRefundSignature(trade, ftx, ra);

            // 3. create payment request
            return Single.zip(refundTxSignature, paymentDetails, (rs, pd) ->
                    new PaymentRequest(ftx.getHashAsString(), pd.getPaymentDetails(), ra.toBase58(), rs));

        }).flatMapMaybe(Single::toMaybe);
    }

//    @Override
//    public Trade handleCreated(BuyerCreated created) {
//        return null;
//    }

    @Override
    public Maybe<Trade> handleFunding(Trade currentTrade, Trade fundingTrade) {
//        Maybe<TransactionWithAmt> tx = walletManager.getEscrowTransactionWithAmt(fundingTrade.getEscrowAddress(), fundingTrade.getFundingTxHash());
//        tx.subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .subscribe(t -> log.debug(t.getDepth().toString()));
        return Maybe.just(fundingTrade);
    }

    // 3.S: seller payout escrow to buyer and write payout details
    public Maybe<Trade> confirmPaymentReceived(Trade paidTrade) {

        Maybe<Trade> completedTrade = Maybe.empty();

        if (paidTrade.status().equals(PAID)) {

            // 1. sign and broadcast payout tx
            //try {
            Maybe<String> payoutTxHash = walletManager.payoutEscrowToBuyer(paidTrade);

            // 2. confirm payout tx and create payout completed
            Maybe<PayoutCompleted> payoutCompleted = payoutTxHash.map(ph -> new PayoutCompleted(ph, PayoutCompleted.Reason.SELLER_BUYER_PAYOUT));

            // 5. post payout completed
//                try {

            completedTrade = payoutCompleted.map(pc -> Trade.builder()
                    .escrowAddress(paidTrade.getEscrowAddress())
                    .sellOffer(paidTrade.sellOffer())
                    .buyRequest(paidTrade.buyRequest())
                    .paymentRequest(paidTrade.paymentRequest())
                    .payoutRequest(paidTrade.payoutRequest())
                    .payoutCompleted(pc)
                    .build());

            //tradeService.put(completedTrade.getEscrowAddress(), completedTrade).subscribe();

//                } catch (IOException e) {
//                    log.error("Can't post payout completed to server.");
//                }

            // TODO handle InsufficientMoneyException
//            } catch (InsufficientMoneyException e) {
//                // TODO notify user
//                log.error("Insufficient funds to payout escrow to buyer.");
//            }
        }

        return completedTrade;
    }

    public void requestArbitrate(Trade currentTrade) {
        super.requestArbitrate(currentTrade, NO_PAYMENT);
    }
}
