package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import lombok.*;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.bytabit.mobile.trade.model.Trade.Role.*;
import static com.bytabit.mobile.trade.model.Trade.Status.*;

@AllArgsConstructor
@Getter
@Builder
@EqualsAndHashCode
@ToString
public class Trade {

    public enum Status {

        CREATED, FUNDING, FUNDED, PAID, COMPLETING, // happy path
        CANCELING, ARBITRATING,
        COMPLETED, CANCELED
    }

    public enum Role {

        BUYER("BUY"), SELLER("SELL"), ARBITRATOR("ARB");

        private String action;

        Role(String action) {
            this.action = action;
        }

        public String getAction() {
            return action;
        }
    }

    @EqualsAndHashCode.Exclude
    @Builder.Default
    private final Long version = 0L;

    @EqualsAndHashCode.Exclude
    private final Status status;

    @EqualsAndHashCode.Exclude
    private final Role role;

    private final String escrowAddress;

    private final LocalDateTime createdTimestamp;

    private final SellOffer sellOffer;

    private final BuyRequest buyRequest;

    @EqualsAndHashCode.Exclude
    private TransactionWithAmt fundingTransactionWithAmt;

    private PaymentRequest paymentRequest;

    private PayoutRequest payoutRequest;

    private ArbitrateRequest arbitrateRequest;

    @EqualsAndHashCode.Exclude
    private TransactionWithAmt payoutTransactionWithAmt;

    private CancelCompleted cancelCompleted;

    private PayoutCompleted payoutCompleted;

    // Sell Offer

    public boolean hasSellOffer() {
        return sellOffer != null;
    }

    public String getArbitratorProfilePubKey() {

        if (hasSellOffer()) {
            return sellOffer.getArbitratorProfilePubKey();
        } else {
            return null;
        }
    }

    public String getSellerProfilePubKey() {

        if (hasSellOffer()) {
            return sellOffer.getSellerProfilePubKey();
        } else {
            return null;
        }
    }

    public String getSellerEscrowPubKey() {

        if (hasSellOffer()) {
            return sellOffer.getSellerEscrowPubKey();
        } else {
            return null;
        }
    }

    public CurrencyCode getCurrencyCode() {
        if (hasSellOffer()) {
            return sellOffer.getCurrencyCode();
        } else {
            return null;
        }
    }

    public PaymentMethod getPaymentMethod() {
        if (hasSellOffer()) {
            return sellOffer.getPaymentMethod();
        } else {
            return null;
        }
    }

    public BigDecimal getPrice() {
        if (hasSellOffer()) {
            return sellOffer.getPrice().setScale(sellOffer.getCurrencyCode().getScale(), RoundingMode.HALF_UP);
        } else {
            return null;
        }
    }

    public BigDecimal getPaymentAmount() {
        if (hasSellOffer() && hasBuyRequest()) {
            return buyRequest.getPaymentAmount().setScale(sellOffer.getCurrencyCode().getScale(), RoundingMode.HALF_UP);
        } else {
            return null;
        }
    }

    // Buy Request

    public boolean hasBuyRequest() {
        return buyRequest != null;
    }

    public String getBuyerProfilePubKey() {

        if (hasBuyRequest()) {
            return buyRequest.getBuyerProfilePubKey();
        } else {
            return null;
        }
    }

    public String getBuyerEscrowPubKey() {

        if (hasBuyRequest()) {
            return buyRequest.getBuyerEscrowPubKey();
        } else {
            return null;
        }
    }

    public BigDecimal getBtcAmount() {
        if (hasBuyRequest()) {
            return buyRequest.getBtcAmount().setScale(8, RoundingMode.HALF_UP);
        } else {
            return null;
        }
    }

    public String getBuyerPayoutAddress() {
        if (hasBuyRequest()) {
            return buyRequest.getBuyerPayoutAddress();
        } else {
            return null;
        }
    }

    // Funding, Payment Request

    public boolean hasPaymentRequest() {
        return paymentRequest != null;
    }

    public String getFundingTxHash() {

        if (hasPaymentRequest()) {
            return paymentRequest.getFundingTxHash();
        } else {
            return null;
        }
    }

    public String getPaymentDetails() {
        if (hasPaymentRequest()) {
            return paymentRequest.getPaymentDetails();
        } else {
            return null;
        }
    }

    public String getRefundAddress() {
        if (hasPaymentRequest()) {
            return paymentRequest.getRefundAddress();
        } else {
            return null;
        }
    }

    public String getRefundTxSignature() {
        if (hasPaymentRequest()) {
            return paymentRequest.getRefundTxSignature();
        } else {
            return null;
        }
    }

    // Payout Request

    public boolean hasPayoutRequest() {
        return payoutRequest != null;
    }

    public String getPaymentReference() {
        if (hasPayoutRequest()) {
            return payoutRequest.getPaymentReference();
        } else {
            return null;
        }
    }

    public String getPayoutTxSignature() {
        if (hasPayoutRequest()) {
            return payoutRequest.getPayoutTxSignature();
        } else {
            return null;
        }
    }

    // Arbitrate Request

    public boolean hasArbitrateRequest() {
        return arbitrateRequest != null;
    }

    public ArbitrateRequest.Reason getArbitrationReason() {
        if (hasArbitrateRequest()) {
            return arbitrateRequest.getReason();
        } else {
            return null;
        }
    }

    // Cancel Completed

    public boolean hasCancelCompleted() {
        return cancelCompleted != null;
    }

    // Payout Completed

    public boolean hasPayoutCompleted() {
        return payoutCompleted != null;
    }

    public String getPayoutTxHash() {
        if (hasPayoutCompleted()) {
            return payoutCompleted.getPayoutTxHash();
        } else if (hasCancelCompleted()) {
            return cancelCompleted.getPayoutTxHash();
        } else {
            return null;
        }
    }

    public PayoutCompleted.Reason getPayoutReason() {
        if (hasPayoutCompleted()) {
            return payoutCompleted.getReason();
        } else {
            return null;
        }
    }

    public Trade.TradeBuilder copyBuilder() {

        return Trade.builder()
                .version(this.version)
                .status(this.status)
                .role(this.role)
                .escrowAddress(this.escrowAddress)
                .createdTimestamp(this.createdTimestamp)
                .sellOffer(this.sellOffer)
                .buyRequest(this.buyRequest)
                .fundingTransactionWithAmt(this.fundingTransactionWithAmt)
                .paymentRequest(this.paymentRequest)
                .payoutRequest(this.payoutRequest)
                .arbitrateRequest(this.arbitrateRequest)
                .payoutTransactionWithAmt(this.payoutTransactionWithAmt)
                .payoutCompleted(this.payoutCompleted)
                .cancelCompleted(this.cancelCompleted);
    }

    public Trade withRole(String profilePubKey) {

        Trade tradeWithRole;

        if (getSellerProfilePubKey().equals(profilePubKey)) {
            tradeWithRole = this.copyBuilder().role(SELLER).build();
        } else if (getBuyerProfilePubKey().equals(profilePubKey)) {
            tradeWithRole = this.copyBuilder().role(BUYER).build();
        } else if (getArbitratorProfilePubKey().equals(profilePubKey)) {
            tradeWithRole = this.copyBuilder().role(ARBITRATOR).build();
        } else {
            throw new TradeManagerException("Unable to determine trade role.");
        }
        return tradeWithRole;
    }

    public Trade withStatus() {

        Trade.Status newStatus = null;
        if (this.getEscrowAddress() != null && hasSellOffer() && hasBuyRequest()) {
            newStatus = CREATED;
        }
        if (newStatus == CREATED && hasPaymentRequest()) {
            newStatus = FUNDING;
        }
        if (newStatus == FUNDING && getFundingTransactionWithAmt() != null && getFundingTransactionWithAmt().getDepth() > 0) {
            newStatus = FUNDED;
        }
        if (newStatus == FUNDED && hasPayoutRequest()) {
            newStatus = PAID;
        }
        if (newStatus == FUNDED && hasPayoutCompleted() && getPayoutCompleted().getReason().equals(PayoutCompleted.Reason.BUYER_SELLER_REFUND)) {
            newStatus = COMPLETING;
        }
        if (hasArbitrateRequest()) {
            newStatus = ARBITRATING;
        }
        if ((newStatus == PAID || newStatus == ARBITRATING || newStatus == CANCELING) && getPayoutTxHash() != null) {
            newStatus = COMPLETING;
        }
        if (newStatus == COMPLETING && getPayoutTransactionWithAmt() != null && getPayoutTransactionWithAmt().getDepth() > 0) {
            newStatus = COMPLETED;
        }
        if (newStatus == CREATED && getCancelCompleted() != null &&
                getCancelCompleted().getReason().equals(CancelCompleted.Reason.CANCEL_CREATED)) {
            newStatus = CANCELED;
        }
        if ((newStatus == FUNDING || newStatus == FUNDED) && getCancelCompleted() != null &&
                getCancelCompleted().getReason().equals(CancelCompleted.Reason.CANCEL_FUNDED)) {
            newStatus = CANCELING;
        }
        if (newStatus == CANCELING && getPayoutTransactionWithAmt() != null && getPayoutTransactionWithAmt().getDepth() > 0) {
            newStatus = CANCELED;
        }

        if (newStatus == null) {
            throw new TradeManagerException("Unable to determine trade status.");
        }
        return this.copyBuilder().status(newStatus).build();
    }
}
