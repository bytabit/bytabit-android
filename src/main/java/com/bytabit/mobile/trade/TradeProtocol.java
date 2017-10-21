package com.bytabit.mobile.trade;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.profile.ProfileManager;
import com.bytabit.mobile.trade.model.ArbitrateRequest;
import com.bytabit.mobile.trade.model.PayoutCompleted;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.wallet.WalletManager;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.inject.Inject;
import java.io.IOException;

import static com.bytabit.mobile.trade.model.PayoutCompleted.Reason.*;
import static com.bytabit.mobile.trade.model.Trade.Status.FUNDED;
import static com.bytabit.mobile.trade.model.Trade.Status.PAID;

public abstract class TradeProtocol {

    protected final Logger log;

    @Inject
    protected WalletManager walletManager;

    @Inject
    protected ProfileManager profileManager;

    protected final TradeService tradeService;

    protected final ObjectMapper objectMapper;

    protected TradeProtocol(Logger log) {

        objectMapper = new ObjectMapper();

        this.log = log;

        Retrofit tradeRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.getBaseUrl())
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
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

        Boolean zeroConfOK = false;
        PayoutCompleted.Reason payoutReason = completedTrade.getPayoutReason();
        Trade.Role tradeRole = completedTrade.getRole(profileManager.getPubKeyProperty().getValue(), profileManager.getIsArbitratorProperty().getValue());
        if ((payoutReason.equals(SELLER_BUYER_PAYOUT) && tradeRole.equals(Trade.Role.SELLER)) ||
                (payoutReason.equals(BUYER_SELLER_REFUND) && tradeRole.equals(Trade.Role.BUYER)) ||
                tradeRole.equals(Trade.Role.ARBITRATOR)) {

            zeroConfOK = true;
        }

        TransactionWithAmt tx = walletManager.getTransactionWithAmt(completedTrade.getEscrowAddress(), txHash, toAddress);
        if (tx != null) {
            if (tx.getBtcAmt().compareTo(completedTrade.getBtcAmount()) == 0) {
                if (zeroConfOK || tx.getDepth() > 0) {

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

            Trade arbitratingTrade = Trade.builder()
                    .escrowAddress(currentTrade.getEscrowAddress())
                    .sellOffer(currentTrade.getSellOffer())
                    .buyRequest(currentTrade.getBuyRequest())
                    .paymentRequest(currentTrade.getPaymentRequest())
                    .payoutRequest(currentTrade.getPayoutRequest())
                    .arbitrateRequest(arbitrateRequest)
                    .build();

            try {
                tradeService.put(currentTrade.getEscrowAddress(), arbitratingTrade).execute();
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
