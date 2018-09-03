package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

public class TradeBuilder {

    private String escrowAddress;
    private Trade.Role role;
    private LocalDateTime createdTimestamp;

    // Sell Offer
    private String sellerEscrowPubKey;
    private String sellerProfilePubKey;
    private String arbitratorProfilePubKey;
    private CurrencyCode currencyCode;
    private PaymentMethod paymentMethod;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal price;

    // Buy Request
    private String buyerEscrowPubKey;
    private BigDecimal btcAmount;
    private String buyerProfilePubKey;
    private String buyerPayoutAddress;

    // Payment Request
    private String fundingTxHash;
    private String paymentDetails;
    private String refundAddress;
    private String refundTxSignature;

    // Funded, Tx Confirmation
    private TransactionWithAmt fundingTransactionWithAmt;

    // Payout Request
    private String paymentReference;
    private String payoutTxSignature;

    // Arbitrate Request
    private ArbitrateRequest.Reason arbitrationReason;

    // Payout Completed
    private String payoutTxHash;
    private PayoutCompleted.Reason payoutReason;

    // Completed, Tx Confirmation
    private TransactionWithAmt payoutTransactionWithAmt;

    public TradeBuilder escrowAddress(String escrowAddress) {
        this.escrowAddress = escrowAddress;
        return this;
    }

    public TradeBuilder role(Trade.Role role) {
        this.role = role;
        return this;
    }

    public TradeBuilder createdTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
        return this;
    }

    public TradeBuilder sellOffer(SellOffer sellOffer) {
        if (sellOffer != null) {
            this.sellerEscrowPubKey = sellOffer.getSellerEscrowPubKey();
            this.sellerProfilePubKey = sellOffer.getSellerProfilePubKey();
            this.arbitratorProfilePubKey = sellOffer.getArbitratorProfilePubKey();
            this.currencyCode = sellOffer.getCurrencyCode();
            this.paymentMethod = sellOffer.getPaymentMethod();
            this.minAmount = sellOffer.getMinAmount();
            this.maxAmount = sellOffer.getMaxAmount();
            this.price = sellOffer.getPrice();
        }
        return this;
    }

    public TradeBuilder buyRequest(BuyRequest buyRequest) {
        if (buyRequest != null) {
            this.buyerEscrowPubKey = buyRequest.getBuyerEscrowPubKey();
            this.btcAmount = buyRequest.getBtcAmount();
            this.buyerProfilePubKey = buyRequest.getBuyerProfilePubKey();
            this.buyerPayoutAddress = buyRequest.getBuyerPayoutAddress();
        }
        return this;
    }

    public TradeBuilder paymentRequest(PaymentRequest paymentRequest) {
        if (paymentRequest != null) {
            this.fundingTxHash = paymentRequest.getFundingTxHash();
            this.paymentDetails = paymentRequest.getPaymentDetails();
            this.refundAddress = paymentRequest.getRefundAddress();
            this.refundTxSignature = paymentRequest.getRefundTxSignature();
        }
        return this;
    }

    public TradeBuilder fundingTransactionWithAmt(TransactionWithAmt transactionWithAmt) {
        if (transactionWithAmt != null) {
            this.fundingTransactionWithAmt = transactionWithAmt;
        }
        return this;
    }

    public TradeBuilder payoutRequest(PayoutRequest payoutRequest) {
        if (payoutRequest != null) {
            this.paymentReference = payoutRequest.getPaymentReference();
            this.payoutTxSignature = payoutRequest.getPayoutTxSignature();
        }
        return this;
    }

    public TradeBuilder arbitrateRequest(ArbitrateRequest arbitrateRequest) {
        if (arbitrateRequest != null) {
            this.arbitrationReason = arbitrateRequest.getReason();
        }
        return this;
    }

    public TradeBuilder payoutCompleted(PayoutCompleted payoutCompleted) {
        if (payoutCompleted != null) {
            this.payoutTxHash = payoutCompleted.getPayoutTxHash();
            this.payoutReason = payoutCompleted.getReason();
        }
        return this;
    }

    public TradeBuilder payoutTransactionWithAmt(TransactionWithAmt transactionWithAmt) {
        if (transactionWithAmt != null) {
            this.payoutTransactionWithAmt = transactionWithAmt;
        }
        return this;
    }

    public Trade build() {
        return new Trade(escrowAddress, role, createdTimestamp, sellerEscrowPubKey, sellerProfilePubKey,
                arbitratorProfilePubKey, currencyCode,
                paymentMethod, minAmount, maxAmount,
                price, buyerEscrowPubKey, btcAmount,
                buyerProfilePubKey, buyerPayoutAddress, fundingTxHash,
                paymentDetails, refundAddress, refundTxSignature,
                paymentReference, payoutTxSignature,
                arbitrationReason, payoutTxHash,
                payoutReason);
    }
}