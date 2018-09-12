package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import lombok.*;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.bytabit.mobile.trade.model.Trade.Status.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"role", "createdTimestamp"})
@ToString
public class Trade {

    public enum Status {

        CREATED, FUNDING, FUNDED, PAID, COMPLETING, // happy path
        CANCELING, ARBITRATING,
        COMPLETED, CANCELED;

        private List<Status> nextValid;

        static {
            CREATED.nextValid = Arrays.asList(FUNDING, CANCELING);
            FUNDING.nextValid = Arrays.asList(FUNDED, CANCELING, ARBITRATING);
            FUNDED.nextValid = Arrays.asList(PAID, CANCELING, ARBITRATING);
            PAID.nextValid = Arrays.asList(COMPLETING, ARBITRATING);
            COMPLETING.nextValid = Arrays.asList(COMPLETED, ARBITRATING);
            CANCELING.nextValid = Arrays.asList(CANCELED, PAID, ARBITRATING);
            ARBITRATING.nextValid = Collections.singletonList(COMPLETED);
            COMPLETED.nextValid = new ArrayList<>();
            CANCELED.nextValid = new ArrayList<>();
        }

        public List<Status> nextValid() {
            return nextValid;
        }
    }

    public enum Role {
        BUYER, SELLER, ARBITRATOR
    }

    private String escrowAddress;

    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private Role role;

    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
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

    // Funding, Payment Request
    private String fundingTxHash;
    private String paymentDetails;
    private String refundAddress;
    private String refundTxSignature;

    // Funded, Tx Confirmation
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
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
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private TransactionWithAmt payoutTransactionWithAmt;

    @Builder
    public Trade(String escrowAddress, LocalDateTime createdTimestamp, Role role, SellOffer sellOffer, BuyRequest buyRequest,
                 PaymentRequest paymentRequest, PayoutRequest payoutRequest, PayoutCompleted payoutCompleted) {

        this.escrowAddress = escrowAddress;
        this.createdTimestamp = createdTimestamp;
        this.role = role;

        this.sellOffer(sellOffer);
        if (buyRequest != null) this.buyRequest(buyRequest);
        if (paymentRequest != null) this.paymentRequest(paymentRequest);
        if (payoutRequest != null) this.payoutRequest(payoutRequest);
        if (payoutCompleted != null) this.payoutCompleted(payoutCompleted);
    }

    public LocalDateTime createdTimestamp() {
        return createdTimestamp;
    }

    public String getCreatedTimestamp() {
        return createdTimestamp.toString();
    }

    public void setCreatedTimestamp(String createdTimestamp) {
        this.createdTimestamp = LocalDateTime.parse(createdTimestamp);
    }

    public Role role() {
        return this.role;
    }

    public void role(Role role) {
        this.role = role;
    }

    public Status status() {

        Status status = null;
        if (this.getEscrowAddress() != null && this.hasSellOffer() && this.hasBuyRequest()) {
            status = CREATED;
        }
        if (status == CREATED && this.hasPaymentRequest()) {
            status = FUNDING;
        }
        if (status == FUNDING && this.fundingTransactionWithAmt() != null && this.fundingTransactionWithAmt().getDepth() > 0) {
            status = FUNDED;
        }
        if (status == FUNDED && this.hasPayoutRequest()) {
            status = PAID;
        }
        if (status == FUNDED && this.hasPayoutCompleted() && this.payoutCompleted().getReason().equals(PayoutCompleted.Reason.BUYER_SELLER_REFUND)) {
            status = COMPLETING;
        }
        if (this.hasArbitrateRequest()) {
            status = ARBITRATING;
        }
        if ((status == PAID || status == ARBITRATING || status == CANCELING) && this.getPayoutTxHash() != null) {
            status = COMPLETING;
        }
        if (status == COMPLETING && this.payoutTransactionWithAmt() != null && this.payoutTransactionWithAmt().getDepth() > 0) {
            status = COMPLETED;
        }
        if (status == null) {
            throw new TradeManagerException("Unable to determine trade status.");
        }
        return status;
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

    public Trade sellOffer(SellOffer sellOffer) {
        this.sellerEscrowPubKey = sellOffer.getSellerEscrowPubKey();
        this.sellerProfilePubKey = sellOffer.getSellerProfilePubKey();
        this.arbitratorProfilePubKey = sellOffer.getArbitratorProfilePubKey();
        this.currencyCode = sellOffer.getCurrencyCode();
        this.paymentMethod = sellOffer.getPaymentMethod();
        this.minAmount = sellOffer.getMinAmount();
        this.maxAmount = sellOffer.getMaxAmount();
        this.price = sellOffer.getPrice();
        return this;
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

    public boolean hasBuyRequest() {
        return buyerEscrowPubKey != null &&
                btcAmount != null &&
                buyerProfilePubKey != null &&
                buyerPayoutAddress != null;
    }

    public Trade buyRequest(BuyRequest buyRequest) {
        this.buyerEscrowPubKey = buyRequest.getBuyerEscrowPubKey();
        this.btcAmount = buyRequest.getBtcAmount();
        this.buyerProfilePubKey = buyRequest.getBuyerProfilePubKey();
        this.buyerPayoutAddress = buyRequest.getBuyerPayoutAddress();
        return this;
    }

    public BuyRequest buyRequest() {
        return new BuyRequest(this.buyerEscrowPubKey, this.btcAmount,
                this.buyerProfilePubKey, this.buyerPayoutAddress);
    }

    public boolean hasPaymentRequest() {
        return fundingTxHash != null &&
                paymentDetails != null &&
                refundAddress != null &&
                refundTxSignature != null;
    }

    public Trade paymentRequest(PaymentRequest paymentRequest) {
        this.fundingTxHash = paymentRequest.getFundingTxHash();
        this.paymentDetails = paymentRequest.getPaymentDetails();
        this.refundAddress = paymentRequest.getRefundAddress();
        this.refundTxSignature = paymentRequest.getRefundTxSignature();
        return this;
    }

    public PaymentRequest paymentRequest() {
        return new PaymentRequest(this.fundingTxHash, this.paymentDetails, this.refundAddress, this.refundTxSignature);
    }

    public boolean hasPayoutRequest() {
        return paymentReference != null && payoutTxSignature != null;
    }

    public Trade payoutRequest(PayoutRequest payoutRequest) {
        this.paymentReference = payoutRequest.getPaymentReference();
        this.payoutTxSignature = payoutRequest.getPayoutTxSignature();
        return this;
    }

    public PayoutRequest payoutRequest() {
        return new PayoutRequest(this.paymentReference, this.payoutTxSignature);
    }

    public boolean hasArbitrateRequest() {
        return arbitrationReason != null;
    }

    public Trade arbitrateRequest(ArbitrateRequest arbitrateRequest) {
        this.arbitrationReason = arbitrateRequest.getReason();
        return this;
    }

    public ArbitrateRequest arbitrateRequest() {
        return new ArbitrateRequest(this.arbitrationReason);
    }

    public boolean hasPayoutCompleted() {
        return payoutTxHash != null &&
                payoutReason != null;
    }

    public Trade payoutCompleted(PayoutCompleted payoutCompleted) {
        this.payoutTxHash = payoutCompleted.getPayoutTxHash();
        this.payoutReason = payoutCompleted.getReason();
        return this;
    }

    public PayoutCompleted payoutCompleted() {
        return new PayoutCompleted(this.payoutTxHash, this.payoutReason);
    }

    public Trade fundingTransactionHash(String fundingTxHash) {
        this.fundingTxHash = fundingTxHash;
        return this;
    }

    public Trade fundingTransactionWithAmt(TransactionWithAmt transactionWithAmt) {
        this.fundingTransactionWithAmt = transactionWithAmt;
        return this;
    }

    public TransactionWithAmt fundingTransactionWithAmt() {
        return fundingTransactionWithAmt;
    }

    public Trade payoutTransactionWithAmt(TransactionWithAmt transactionWithAmt) {
        this.payoutTransactionWithAmt = transactionWithAmt;
        return this;
    }

    public TransactionWithAmt payoutTransactionWithAmt() {
        return payoutTransactionWithAmt;
    }

}
