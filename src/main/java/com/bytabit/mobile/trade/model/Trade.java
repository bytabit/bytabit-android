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
public class Trade {

    public enum Status {
        CREATED, FUNDED, PAID, COMPLETED, ARBITRATING
    }

    public enum Role {
        BUYER, SELLER, ARBITRATOR
    }

    @Getter
    @Setter
    private String escrowAddress;

    // Sell Offer
    @Getter
    @Setter
    private String sellerEscrowPubKey;
    @Getter
    @Setter
    private String sellerProfilePubKey;
    @Getter
    @Setter
    private String arbitratorProfilePubKey;
    @Getter
    @Setter
    private CurrencyCode currencyCode;
    @Getter
    @Setter
    private PaymentMethod paymentMethod;
    @Getter
    @Setter
    private BigDecimal minAmount;
    @Getter
    @Setter
    private BigDecimal maxAmount;
    @Getter
    @Setter
    private BigDecimal price;

    // Buy Request
    @Getter
    @Setter
    private String buyerEscrowPubKey;
    @Getter
    @Setter
    private BigDecimal btcAmount;
    @Getter
    @Setter
    private String buyerProfilePubKey;
    @Getter
    @Setter
    private String buyerPayoutAddress;

    // Payment Request
    @Getter
    @Setter
    private String fundingTxHash;
    @Getter
    @Setter
    private String paymentDetails;
    @Getter
    @Setter
    private String refundAddress;
    @Getter
    @Setter
    private String refundTxSignature;

    // Payout Request
    @Getter
    @Setter
    private String paymentReference;
    @Getter
    @Setter
    private String payoutTxSignature;

    // Arbitrate Request
    @Getter
    @Setter
    private ArbitrateRequest.Reason arbitrationReason;

    // Payout Completed
    @Getter
    @Setter
    private String payoutTxHash;
    @Getter
    @Setter
    private PayoutCompleted.Reason payoutReason;

    @Builder
    Trade(String escrowAddress, SellOffer sellOffer, BuyRequest buyRequest,
          PaymentRequest paymentRequest, PayoutRequest payoutRequest,
          ArbitrateRequest arbitrateRequest, PayoutCompleted payoutCompleted) {

        this.escrowAddress = escrowAddress;
        sellOffer(sellOffer);
        buyRequest(buyRequest);
        paymentRequest(paymentRequest);
        payoutRequest(payoutRequest);
        arbitrateRequest(arbitrateRequest);
        payoutCompleted(payoutCompleted);
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

    private void sellOffer(SellOffer sellOffer) {
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

    private boolean hasBuyRequest() {
        return buyerEscrowPubKey != null &&
                btcAmount != null &&
                buyerProfilePubKey != null &&
                buyerPayoutAddress != null;
    }

    public BuyRequest buyRequest() {
        return BuyRequest.builder()
                .buyerEscrowPubKey(this.buyerEscrowPubKey)
                .btcAmount(this.btcAmount)
                .buyerProfilePubKey(this.buyerProfilePubKey)
                .buyerPayoutAddress(this.buyerPayoutAddress)
                .build();
    }

    private void buyRequest(BuyRequest buyRequest) {
        if (buyRequest != null) {
            this.buyerEscrowPubKey = buyRequest.getBuyerEscrowPubKey();
            this.btcAmount = buyRequest.getBtcAmount();
            this.buyerProfilePubKey = buyRequest.getBuyerProfilePubKey();
            this.buyerPayoutAddress = buyRequest.getBuyerPayoutAddress();
        }
    }

    private boolean hasPaymentRequest() {
        return fundingTxHash != null &&
                paymentDetails != null &&
                refundAddress != null &&
                refundTxSignature != null;
    }

    public PaymentRequest paymentRequest() {
        return PaymentRequest.builder()
                .fundingTxHash(this.fundingTxHash)
                .paymentDetails(this.paymentDetails)
                .refundAddress(this.refundAddress)
                .refundTxSignature(this.refundTxSignature)
                .build();
    }

    private void paymentRequest(PaymentRequest paymentRequest) {
        if (paymentRequest != null) {
            this.fundingTxHash = paymentRequest.getFundingTxHash();
            this.paymentDetails = paymentRequest.getPaymentDetails();
            this.refundAddress = paymentRequest.getRefundAddress();
            this.refundTxSignature = paymentRequest.getRefundTxSignature();
        }
    }

    public boolean hasPayoutRequest() {
        return paymentReference != null &&
                payoutTxSignature != null;
    }

    public PayoutRequest payoutRequest() {
        return PayoutRequest.builder()
                .paymentReference(this.paymentReference)
                .payoutTxSignature(this.payoutTxSignature)
                .build();
    }

    private void payoutRequest(PayoutRequest payoutRequest) {
        if (payoutRequest != null) {
            this.paymentReference = payoutRequest.getPaymentReference();
            this.payoutTxSignature = payoutRequest.getPayoutTxSignature();
        }
    }

    public boolean hasArbitrateRequest() {
        return arbitrationReason != null;
    }

//    public ArbitrateRequest arbitrateRequest() {
//        return ArbitrateRequest.builder()
//                .reason(this.arbitrationReason)
//                .build();
//    }

    private void arbitrateRequest(ArbitrateRequest arbitrateRequest) {
        if (arbitrateRequest != null) {
            this.arbitrationReason = arbitrateRequest.getReason();
        }
    }

    public boolean hasPayoutCompleted() {
        return payoutTxHash != null &&
                payoutReason != null;
    }

//    public PayoutCompleted payoutCompleted() {
//        return PayoutCompleted.builder()
//                .payoutTxHash(this.payoutTxHash)
//                .reason(this.payoutReason)
//                .build();
//    }

    private void payoutCompleted(PayoutCompleted payoutCompleted) {
        if (payoutCompleted != null) {
            this.payoutTxHash = payoutCompleted.getPayoutTxHash();
            this.payoutReason = payoutCompleted.getReason();
        }
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
}
