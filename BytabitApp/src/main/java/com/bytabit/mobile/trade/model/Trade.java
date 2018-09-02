package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.bytabit.mobile.trade.model.Trade.Status.*;

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
    private Role role;

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

    public Trade() {
    }

    public static TradeBuilder builder() {
        return new TradeBuilder();
    }

    Trade(String escrowAddress, Role role, String sellerEscrowPubKey, String sellerProfilePubKey,
          String arbitratorProfilePubKey, CurrencyCode currencyCode,
          PaymentMethod paymentMethod, BigDecimal minAmount, BigDecimal maxAmount,
          BigDecimal price, String buyerEscrowPubKey, BigDecimal btcAmount,
          String buyerProfilePubKey, String buyerPayoutAddress, String fundingTxHash,
          String paymentDetails, String refundAddress, String refundTxSignature,
          String paymentReference, String payoutTxSignature,
          ArbitrateRequest.Reason arbitrationReason, String payoutTxHash,
          PayoutCompleted.Reason payoutReason) {

        this.escrowAddress = escrowAddress;
        this.role = role;
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

    public Role getRole() {
        return role;
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

    public void setRole(Role role) {
        this.role = role;
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
        if (status == PAID && this.getPayoutTxHash() != null) {
            status = COMPLETING;
        }
        if (status == COMPLETING && this.payoutTransactionWithAmt() != null && this.payoutTransactionWithAmt().getDepth() > 0) {
            status = COMPLETED;
        }
        if (status == null) {
            throw new RuntimeException("Unable to determine trade status.");
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

//    public Trade role(String profilePubKey, Boolean getIsArbitrator) {
//
//        if (!getIsArbitrator) {
//            if (getSellerProfilePubKey().equals(profilePubKey)) {
//                this.role = SELLER;
//            } else if (getBuyerProfilePubKey().equals(profilePubKey)) {
//                this.role = BUYER;
//            } else {
//                throw new RuntimeException("Unable to determine trader role.");
//            }
//        } else if (getArbitratorProfilePubKey().equals(profilePubKey)) {
//            this.role = ARBITRATOR;
//        } else {
//            throw new RuntimeException("Unable to determine arbitrator role.");
//        }
//        return this;
//    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Trade trade = (Trade) o;

        if (escrowAddress != null ? !escrowAddress.equals(trade.escrowAddress) : trade.escrowAddress != null)
            return false;
        if (sellerEscrowPubKey != null ? !sellerEscrowPubKey.equals(trade.sellerEscrowPubKey) : trade.sellerEscrowPubKey != null)
            return false;
        if (sellerProfilePubKey != null ? !sellerProfilePubKey.equals(trade.sellerProfilePubKey) : trade.sellerProfilePubKey != null)
            return false;
        if (arbitratorProfilePubKey != null ? !arbitratorProfilePubKey.equals(trade.arbitratorProfilePubKey) : trade.arbitratorProfilePubKey != null)
            return false;
        if (currencyCode != trade.currencyCode) return false;
        if (paymentMethod != trade.paymentMethod) return false;
        if (minAmount != null ? !minAmount.equals(trade.minAmount) : trade.minAmount != null)
            return false;
        if (maxAmount != null ? !maxAmount.equals(trade.maxAmount) : trade.maxAmount != null)
            return false;
        if (price != null ? !price.equals(trade.price) : trade.price != null)
            return false;
        if (buyerEscrowPubKey != null ? !buyerEscrowPubKey.equals(trade.buyerEscrowPubKey) : trade.buyerEscrowPubKey != null)
            return false;
        if (btcAmount != null ? !btcAmount.equals(trade.btcAmount) : trade.btcAmount != null)
            return false;
        if (buyerProfilePubKey != null ? !buyerProfilePubKey.equals(trade.buyerProfilePubKey) : trade.buyerProfilePubKey != null)
            return false;
        if (buyerPayoutAddress != null ? !buyerPayoutAddress.equals(trade.buyerPayoutAddress) : trade.buyerPayoutAddress != null)
            return false;
        if (fundingTxHash != null ? !fundingTxHash.equals(trade.fundingTxHash) : trade.fundingTxHash != null)
            return false;
        if (paymentDetails != null ? !paymentDetails.equals(trade.paymentDetails) : trade.paymentDetails != null)
            return false;
        if (refundAddress != null ? !refundAddress.equals(trade.refundAddress) : trade.refundAddress != null)
            return false;
        if (refundTxSignature != null ? !refundTxSignature.equals(trade.refundTxSignature) : trade.refundTxSignature != null)
            return false;
        if (fundingTransactionWithAmt != null ? !fundingTransactionWithAmt.equals(trade.fundingTransactionWithAmt) : trade.fundingTransactionWithAmt != null)
            return false;
        if (paymentReference != null ? !paymentReference.equals(trade.paymentReference) : trade.paymentReference != null)
            return false;
        if (payoutTxSignature != null ? !payoutTxSignature.equals(trade.payoutTxSignature) : trade.payoutTxSignature != null)
            return false;
        if (arbitrationReason != trade.arbitrationReason) return false;
        if (payoutTxHash != null ? !payoutTxHash.equals(trade.payoutTxHash) : trade.payoutTxHash != null)
            return false;
        if (payoutReason != trade.payoutReason) return false;
        return payoutTransactionWithAmt != null ? payoutTransactionWithAmt.equals(trade.payoutTransactionWithAmt) : trade.payoutTransactionWithAmt == null;
    }

    @Override
    public int hashCode() {
        int result = escrowAddress != null ? escrowAddress.hashCode() : 0;
        result = 31 * result + (sellerEscrowPubKey != null ? sellerEscrowPubKey.hashCode() : 0);
        result = 31 * result + (sellerProfilePubKey != null ? sellerProfilePubKey.hashCode() : 0);
        result = 31 * result + (arbitratorProfilePubKey != null ? arbitratorProfilePubKey.hashCode() : 0);
        result = 31 * result + (currencyCode != null ? currencyCode.hashCode() : 0);
        result = 31 * result + (paymentMethod != null ? paymentMethod.hashCode() : 0);
        result = 31 * result + (minAmount != null ? minAmount.hashCode() : 0);
        result = 31 * result + (maxAmount != null ? maxAmount.hashCode() : 0);
        result = 31 * result + (price != null ? price.hashCode() : 0);
        result = 31 * result + (buyerEscrowPubKey != null ? buyerEscrowPubKey.hashCode() : 0);
        result = 31 * result + (btcAmount != null ? btcAmount.hashCode() : 0);
        result = 31 * result + (buyerProfilePubKey != null ? buyerProfilePubKey.hashCode() : 0);
        result = 31 * result + (buyerPayoutAddress != null ? buyerPayoutAddress.hashCode() : 0);
        result = 31 * result + (fundingTxHash != null ? fundingTxHash.hashCode() : 0);
        result = 31 * result + (paymentDetails != null ? paymentDetails.hashCode() : 0);
        result = 31 * result + (refundAddress != null ? refundAddress.hashCode() : 0);
        result = 31 * result + (refundTxSignature != null ? refundTxSignature.hashCode() : 0);
        result = 31 * result + (fundingTransactionWithAmt != null ? fundingTransactionWithAmt.hashCode() : 0);
        result = 31 * result + (paymentReference != null ? paymentReference.hashCode() : 0);
        result = 31 * result + (payoutTxSignature != null ? payoutTxSignature.hashCode() : 0);
        result = 31 * result + (arbitrationReason != null ? arbitrationReason.hashCode() : 0);
        result = 31 * result + (payoutTxHash != null ? payoutTxHash.hashCode() : 0);
        result = 31 * result + (payoutReason != null ? payoutReason.hashCode() : 0);
        result = 31 * result + (payoutTransactionWithAmt != null ? payoutTransactionWithAmt.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Trade{");
        sb.append("escrowAddress='").append(escrowAddress).append('\'');
        sb.append(", sellerEscrowPubKey='").append(sellerEscrowPubKey).append('\'');
        sb.append(", sellerProfilePubKey='").append(sellerProfilePubKey).append('\'');
        sb.append(", arbitratorProfilePubKey='").append(arbitratorProfilePubKey).append('\'');
        sb.append(", currencyCode=").append(currencyCode);
        sb.append(", paymentMethod=").append(paymentMethod);
        sb.append(", minAmount=").append(minAmount);
        sb.append(", maxAmount=").append(maxAmount);
        sb.append(", price=").append(price);
        sb.append(", buyerEscrowPubKey='").append(buyerEscrowPubKey).append('\'');
        sb.append(", btcAmount=").append(btcAmount);
        sb.append(", buyerProfilePubKey='").append(buyerProfilePubKey).append('\'');
        sb.append(", buyerPayoutAddress='").append(buyerPayoutAddress).append('\'');
        sb.append(", fundingTxHash='").append(fundingTxHash).append('\'');
        sb.append(", paymentDetails='").append(paymentDetails).append('\'');
        sb.append(", refundAddress='").append(refundAddress).append('\'');
        sb.append(", refundTxSignature='").append(refundTxSignature).append('\'');
        sb.append(", fundingTransactionWithAmt=").append(fundingTransactionWithAmt);
        sb.append(", paymentReference='").append(paymentReference).append('\'');
        sb.append(", payoutTxSignature='").append(payoutTxSignature).append('\'');
        sb.append(", arbitrationReason=").append(arbitrationReason);
        sb.append(", payoutTxHash='").append(payoutTxHash).append('\'');
        sb.append(", payoutReason=").append(payoutReason);
        sb.append(", payoutTransactionWithAmt=").append(payoutTransactionWithAmt);
        sb.append('}');
        return sb.toString();
    }
}
