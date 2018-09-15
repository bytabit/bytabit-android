package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

import static com.bytabit.mobile.trade.model.Trade.Role.*;
import static com.bytabit.mobile.trade.model.Trade.Status.*;

@AllArgsConstructor
@Getter
@Builder
public class Trade {

    public enum Status {

        CREATED, FUNDING, FUNDED, PAID, COMPLETING, // happy path
        CANCELING, ARBITRATING,
        COMPLETED, CANCELED
    }

    public enum Role {
        BUYER, SELLER, ARBITRATOR
    }

    private final Long version;

    private final Status status;

    private final Role role;

    private final String escrowAddress;

    private final LocalDateTime createdTimestamp;

    // Sell Offer
    private final SellOffer sellOffer;

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
            return sellOffer.getPrice();
        } else {
            return null;
        }
    }

    // Buy Request
    private final BuyRequest buyRequest;

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
            return buyRequest.getBtcAmount().stripTrailingZeros();
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

    // Funded, Tx Confirmation
    private TransactionWithAmt fundingTransactionWithAmt;

    // Funding, Payment Request
    private PaymentRequest paymentRequest;

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
    private PayoutRequest payoutRequest;

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
    private ArbitrateRequest arbitrateRequest;

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

    // Completed, Tx Confirmation
    private TransactionWithAmt payoutTransactionWithAmt;

    // Payout Completed
    private PayoutCompleted payoutCompleted;

    public boolean hasPayoutCompleted() {
        return payoutCompleted != null;
    }

    public String getPayoutTxHash() {
        if (hasPayoutCompleted()) {
            return payoutCompleted.getPayoutTxHash();
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
                .payoutCompleted(this.payoutCompleted);
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

        Trade.Status status = null;
        if (this.getEscrowAddress() != null && hasSellOffer() && hasBuyRequest()) {
            status = CREATED;
        }
        if (status == CREATED && hasPaymentRequest()) {
            status = FUNDING;
        }
        if (status == FUNDING && getFundingTransactionWithAmt() != null && getFundingTransactionWithAmt().getDepth() > 0) {
            status = FUNDED;
        }
        if (status == FUNDED && hasPayoutRequest()) {
            status = PAID;
        }
        if (status == FUNDED && hasPayoutCompleted() && getPayoutCompleted().getReason().equals(PayoutCompleted.Reason.BUYER_SELLER_REFUND)) {
            status = COMPLETING;
        }
        if (hasArbitrateRequest()) {
            status = ARBITRATING;
        }
        if ((status == PAID || status == ARBITRATING || status == CANCELING) && getPayoutTxHash() != null) {
            status = COMPLETING;
        }
        if (status == COMPLETING && getPayoutTransactionWithAmt() != null && getPayoutTransactionWithAmt().getDepth() > 0) {
            status = COMPLETED;
        }
        if (status == null) {
            throw new TradeManagerException("Unable to determine trade status.");
        }
        return this.copyBuilder().status(status).build();
    }
}
