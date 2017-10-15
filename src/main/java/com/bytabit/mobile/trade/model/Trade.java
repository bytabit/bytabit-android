package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;

import static com.bytabit.mobile.trade.model.Trade.Role.*;
import static com.bytabit.mobile.trade.model.Trade.Status.*;

@NoArgsConstructor
@EqualsAndHashCode(of = "escrowAddress")
@Getter
@Setter(AccessLevel.PACKAGE)
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

    @Builder
    public Trade(String escrowAddress, SellOffer sellOffer, BuyRequest buyRequest,
                 PaymentRequest paymentRequest, PayoutRequest payoutRequest,
                 ArbitrateRequest arbitrateRequest, PayoutCompleted payoutCompleted) {

        this.escrowAddress = escrowAddress;
        setSellOffer(sellOffer);
        setBuyRequest(buyRequest);
        setPaymentRequest(paymentRequest);
        setPayoutRequest(payoutRequest);
        setArbitrateRequest(arbitrateRequest);
        setPayoutCompleted(payoutCompleted);
    }

    public Status getStatus() {
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

    private void setSellOffer(SellOffer sellOffer) {
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
    }

    public boolean hasSellOffer() {
        return sellerEscrowPubKey != null &&
                sellerProfilePubKey != null &&
                arbitratorProfilePubKey != null &&
                currencyCode != null &&
                paymentMethod != null &&
                minAmount != null &&
                maxAmount != null &&
                price != null;
    }

    private void setBuyRequest(BuyRequest buyRequest) {
        if (buyRequest != null) {
            this.buyerEscrowPubKey = buyRequest.getBuyerEscrowPubKey();
            this.btcAmount = buyRequest.getBtcAmount();
            this.buyerProfilePubKey = buyRequest.getBuyerProfilePubKey();
            this.buyerPayoutAddress = buyRequest.getBuyerPayoutAddress();
        }
    }

    public boolean hasBuyRequest() {
        return buyerEscrowPubKey != null &&
                btcAmount != null &&
                buyerProfilePubKey != null &&
                buyerPayoutAddress != null;
    }

    private void setPaymentRequest(PaymentRequest paymentRequest) {
        if (paymentRequest != null) {
            this.fundingTxHash = paymentRequest.getFundingTxHash();
            this.paymentDetails = paymentRequest.getPaymentDetails();
            this.refundAddress = paymentRequest.getRefundAddress();
            this.refundTxSignature = paymentRequest.getRefundTxSignature();
        }
    }

    public boolean hasPaymentRequest() {
        return fundingTxHash != null &&
                paymentDetails != null &&
                refundAddress != null &&
                refundTxSignature != null;
    }

    private void setPayoutRequest(PayoutRequest payoutRequest) {
        if (payoutRequest != null) {
            this.paymentReference = payoutRequest.getPaymentReference();
            this.payoutTxSignature = payoutRequest.getPayoutTxSignature();
        }
    }

    public boolean hasPayoutRequest() {
        return paymentReference != null &&
                payoutTxSignature != null;
    }

    private void setArbitrateRequest(ArbitrateRequest arbitrateRequest) {
        if (arbitrateRequest != null) {
            this.arbitrationReason = arbitrateRequest.getReason();
        }
    }

    public boolean hasArbitrateRequest() {
        return arbitrationReason != null;
    }

    private void setPayoutCompleted(PayoutCompleted payoutCompleted) {
        if (payoutCompleted != null) {
            this.payoutTxHash = payoutCompleted.getPayoutTxHash();
            this.payoutReason = payoutCompleted.getReason();
        }
    }

    public boolean hasPayoutCompleted() {
        return payoutTxHash != null &&
                payoutReason != null;
    }

    public Role getRole(String profilePubKey, Boolean isArbitrator) {
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
}
