package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;

import java.math.BigDecimal;

import static com.bytabit.mobile.trade.model.Trade.Role.*;
import static com.bytabit.mobile.trade.model.Trade.Status.*;

public class Trade {

    public enum Status {
        CREATED, FUNDED, PAID, COMPLETED, ARBITRATING
    }

    public enum Role {
        BUYER, SELLER, ARBITRATOR
    }

    private String escrowAddress;

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

    // Payout Request
    private String paymentReference;
    private String payoutTxSignature;

    // Arbitrate Request
    private ArbitrateRequest.Reason arbitrationReason;

    // Payout Completed
    private String payoutTxHash;
    private PayoutCompleted.Reason payoutReason;

    public Trade() {
    }

    public static TradeBuilder builder() {
        return new TradeBuilder();
    }

    Trade(String escrowAddress, String sellerEscrowPubKey, String sellerProfilePubKey,
          String arbitratorProfilePubKey, CurrencyCode currencyCode,
          PaymentMethod paymentMethod, BigDecimal minAmount, BigDecimal maxAmount,
          BigDecimal price, String buyerEscrowPubKey, BigDecimal btcAmount,
          String buyerProfilePubKey, String buyerPayoutAddress, String fundingTxHash,
          String paymentDetails, String refundAddress, String refundTxSignature,
          String paymentReference, String payoutTxSignature,
          ArbitrateRequest.Reason arbitrationReason, String payoutTxHash,
          PayoutCompleted.Reason payoutReason) {

        this.escrowAddress = escrowAddress;
        this.sellerEscrowPubKey = sellerEscrowPubKey;
        this.sellerProfilePubKey = sellerProfilePubKey;
        this.arbitratorProfilePubKey = arbitratorProfilePubKey;
        this.currencyCode = currencyCode;
        this.paymentMethod = paymentMethod;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.price = price;
        this.buyerEscrowPubKey = buyerEscrowPubKey;
        this.btcAmount = btcAmount;
        this.buyerProfilePubKey = buyerProfilePubKey;
        this.buyerPayoutAddress = buyerPayoutAddress;
        this.fundingTxHash = fundingTxHash;
        this.paymentDetails = paymentDetails;
        this.refundAddress = refundAddress;
        this.refundTxSignature = refundTxSignature;
        this.paymentReference = paymentReference;
        this.payoutTxSignature = payoutTxSignature;
        this.arbitrationReason = arbitrationReason;
        this.payoutTxHash = payoutTxHash;
        this.payoutReason = payoutReason;
    }

    public String getEscrowAddress() {
        return escrowAddress;
    }

    public String getSellerEscrowPubKey() {
        return sellerEscrowPubKey;
    }

    public String getSellerProfilePubKey() {
        return sellerProfilePubKey;
    }

    public String getArbitratorProfilePubKey() {
        return arbitratorProfilePubKey;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getBuyerEscrowPubKey() {
        return buyerEscrowPubKey;
    }

    public BigDecimal getBtcAmount() {
        return btcAmount;
    }

    public String getBuyerProfilePubKey() {
        return buyerProfilePubKey;
    }

    public String getBuyerPayoutAddress() {
        return buyerPayoutAddress;
    }

    public String getFundingTxHash() {
        return fundingTxHash;
    }

    public String getPaymentDetails() {
        return paymentDetails;
    }

    public String getRefundAddress() {
        return refundAddress;
    }

    public String getRefundTxSignature() {
        return refundTxSignature;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getPayoutTxSignature() {
        return payoutTxSignature;
    }

    public ArbitrateRequest.Reason getArbitrationReason() {
        return arbitrationReason;
    }

    public String getPayoutTxHash() {
        return payoutTxHash;
    }

    public PayoutCompleted.Reason getPayoutReason() {
        return payoutReason;
    }

    public void setEscrowAddress(String escrowAddress) {
        this.escrowAddress = escrowAddress;
    }

    public void setSellerEscrowPubKey(String sellerEscrowPubKey) {
        this.sellerEscrowPubKey = sellerEscrowPubKey;
    }

    public void setSellerProfilePubKey(String sellerProfilePubKey) {
        this.sellerProfilePubKey = sellerProfilePubKey;
    }

    public void setArbitratorProfilePubKey(String arbitratorProfilePubKey) {
        this.arbitratorProfilePubKey = arbitratorProfilePubKey;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setBuyerEscrowPubKey(String buyerEscrowPubKey) {
        this.buyerEscrowPubKey = buyerEscrowPubKey;
    }

    public void setBtcAmount(BigDecimal btcAmount) {
        this.btcAmount = btcAmount;
    }

    public void setBuyerProfilePubKey(String buyerProfilePubKey) {
        this.buyerProfilePubKey = buyerProfilePubKey;
    }

    public void setBuyerPayoutAddress(String buyerPayoutAddress) {
        this.buyerPayoutAddress = buyerPayoutAddress;
    }

    public void setFundingTxHash(String fundingTxHash) {
        this.fundingTxHash = fundingTxHash;
    }

    public void setPaymentDetails(String paymentDetails) {
        this.paymentDetails = paymentDetails;
    }

    public void setRefundAddress(String refundAddress) {
        this.refundAddress = refundAddress;
    }

    public void setRefundTxSignature(String refundTxSignature) {
        this.refundTxSignature = refundTxSignature;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public void setPayoutTxSignature(String payoutTxSignature) {
        this.payoutTxSignature = payoutTxSignature;
    }

    public void setArbitrationReason(ArbitrateRequest.Reason arbitrationReason) {
        this.arbitrationReason = arbitrationReason;
    }

    public void setPayoutTxHash(String payoutTxHash) {
        this.payoutTxHash = payoutTxHash;
    }

    public void setPayoutReason(PayoutCompleted.Reason payoutReason) {
        this.payoutReason = payoutReason;
    }

    public Status status() {
        Status status = null;
        if (escrowAddress != null && hasSellOffer() && hasBuyRequest()) {
            status = CREATED;
        }
        if (status == CREATED && hasPaymentRequest()) {
            status = FUNDED;
        }
        if (status == FUNDED && hasPayoutRequest()) {
            status = PAID;
        }
        if (hasArbitrateRequest()) {
            status = ARBITRATING;
        }
        if (hasPayoutCompleted()) {
            status = COMPLETED;
        }
        return status;
    }

    private boolean hasSellOffer() {
        return sellerEscrowPubKey != null &&
                sellerProfilePubKey != null &&
                arbitratorProfilePubKey != null &&
                currencyCode != null &&
                paymentMethod != null &&
                minAmount != null &&
                maxAmount != null &&
                price != null;
    }

    public SellOffer sellOffer() {
        return SellOffer.builder()
                .sellerEscrowPubKey(this.sellerEscrowPubKey)
                .sellerProfilePubKey(this.sellerProfilePubKey)
                .arbitratorProfilePubKey(this.arbitratorProfilePubKey)
                .currencyCode(this.currencyCode)
                .paymentMethod(this.paymentMethod)
                .minAmount(this.minAmount)
                .maxAmount(this.maxAmount)
                .price(this.price)
                .build();
    }

    private boolean hasBuyRequest() {
        return buyerEscrowPubKey != null &&
                btcAmount != null &&
                buyerProfilePubKey != null &&
                buyerPayoutAddress != null;
    }

    public BuyRequest buyRequest() {
        return new BuyRequest(this.buyerEscrowPubKey, this.btcAmount, this.buyerProfilePubKey, this.buyerPayoutAddress);
    }

    private boolean hasPaymentRequest() {
        return fundingTxHash != null &&
                paymentDetails != null &&
                refundAddress != null &&
                refundTxSignature != null;
    }

    public PaymentRequest paymentRequest() {
        return new PaymentRequest(this.fundingTxHash, this.paymentDetails, this.refundAddress, this.refundTxSignature);
    }

    public boolean hasPayoutRequest() {
        return paymentReference != null &&
                payoutTxSignature != null;
    }

    public PayoutRequest payoutRequest() {
        return new PayoutRequest(this.paymentReference, this.payoutTxSignature);
    }

    public boolean hasArbitrateRequest() {
        return arbitrationReason != null;
    }

    public ArbitrateRequest arbitrateRequest() {
        return new ArbitrateRequest(this.arbitrationReason);
    }

    public boolean hasPayoutCompleted() {
        return payoutTxHash != null &&
                payoutReason != null;
    }

    public PayoutCompleted payoutCompleted() {
        return new PayoutCompleted(this.payoutTxHash, this.payoutReason);
    }

    public Role role(String profilePubKey, Boolean isArbitrator) {
        Role role;

        if (!isArbitrator) {
            if (getSellerProfilePubKey().equals(profilePubKey)) {
                role = SELLER;
            } else if (getBuyerProfilePubKey().equals(profilePubKey)) {
                role = BUYER;
            } else {
                throw new RuntimeException("Unable to determine trader role.");
            }
        } else if (getArbitratorProfilePubKey().equals(profilePubKey)) {
            role = ARBITRATOR;
        } else {
            throw new RuntimeException("Unable to determine arbitrator role.");
        }

        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Trade trade = (Trade) o;

        return escrowAddress != null ? escrowAddress.equals(trade.escrowAddress) : trade.escrowAddress == null;
    }

    @Override
    public int hashCode() {
        return escrowAddress != null ? escrowAddress.hashCode() : 0;
    }
}
