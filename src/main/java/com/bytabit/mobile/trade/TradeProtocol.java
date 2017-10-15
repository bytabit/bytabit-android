package com.bytabit.mobile.trade;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.trade.model.ArbitrateRequest;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.fasterxml.jackson.jr.retrofit2.JacksonJrConverter;
import org.slf4j.Logger;
import retrofit2.Retrofit;

import javax.inject.Inject;
import java.io.IOException;

import static com.bytabit.mobile.trade.model.PayoutCompleted.Reason.ARBITRATOR_SELLER_REFUND;
import static com.bytabit.mobile.trade.model.PayoutCompleted.Reason.BUYER_SELLER_REFUND;
import static com.bytabit.mobile.trade.model.Trade.Status.FUNDED;
import static com.bytabit.mobile.trade.model.Trade.Status.PAID;

public abstract class TradeProtocol {

    protected final Logger log;

    @Inject
    protected WalletManager walletManager;

    protected final TradeService tradeService;

    protected TradeProtocol(Logger log) {
        this.log = log;

        Retrofit tradeRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(new JacksonJrConverter<>(Trade.class))
                .build();
        tradeService = tradeRetrofit.create(TradeService.class);
    }

    // CREATED, FUNDED, PAID, COMPLETED, ARBITRATING

    abstract public Trade handleCreated(Trade createdTrade);

    abstract public Trade handleFunded(Trade createdTrade, Trade fundedTrade);

    public Trade handlePaid(Trade fundedTrade, Trade paidTrade) {

        // TODO handle unexpected status
        if (fundedTrade.getStatus().equals(FUNDED)) {
            return paidTrade;
        } else {
            return null;
        }
    }

    public Trade handleCompleted(Trade currentTrade, Trade completedTrade) {

        Trade verifiedCompletedTrade = null;

        // confirm payout tx
        String txHash = completedTrade.getPayoutTxHash();
        String toAddress = completedTrade.getBuyerPayoutAddress();

        // if arbitrated refund to seller, verify outputs to refund address
        if (completedTrade.getPayoutReason().equals(ARBITRATOR_SELLER_REFUND) ||
                completedTrade.getPayoutReason().equals(BUYER_SELLER_REFUND)) {
            toAddress = completedTrade.getRefundAddress();
        }

        TransactionWithAmt tx = walletManager.getTransactionWithAmt(completedTrade.getEscrowAddress(), txHash, toAddress);
        if (tx != null) {
            if (tx.getBtcAmt().equals(completedTrade.getBtcAmount())) {
                if (tx.getDepth() > 0) {

                    verifiedCompletedTrade = completedTrade;

                    // remove watch on escrow and refund addresses
                    walletManager.removeWatchedEscrowAddress(completedTrade.getEscrowAddress());
                    //walletManager.removeWatchedEscrowAddress(trade.getPaymentRequest().getRefundAddress());
                    //walletManager.removeWatchedEscrowAddress(trade.getBuyRequest().getBuyerPayoutAddress());

                } else {
                    log.info("PayoutCompleted Tx depth not yet greater than 1.");
                }
            } else {
                log.error("Tx amount wrong for PayoutCompleted.");
            }
        } else {
            log.error("Tx not found for PayoutCompleted.");
        }

        return verifiedCompletedTrade;
    }

    protected void requestArbitrate(Trade currentTrade, ArbitrateRequest.Reason reason) {

        if (currentTrade.getStatus().equals(FUNDED) || currentTrade.getStatus().equals(PAID)) {

            ArbitrateRequest arbitrateRequest = ArbitrateRequest.builder()
                    .reason(reason)
                    .build();

            try {
                tradeService.put(currentTrade.getEscrowAddress(), arbitrateRequest).execute();
            } catch (IOException e) {
                log.error("Can't post ArbitrateRequest to server.");
            }
        }
    }

    public Trade handleArbitrating(Trade currentTrade, Trade arbitratingTrade) {

        // TODO handle unexpected status
        if (currentTrade.getStatus().equals(FUNDED) || currentTrade.getStatus().equals(PAID)) {
            return arbitratingTrade;
        } else {
            return null;
        }
    }
}
