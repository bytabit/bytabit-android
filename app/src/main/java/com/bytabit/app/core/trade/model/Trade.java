/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.app.core.trade.model;

import com.bytabit.app.core.common.HashUtils;
import com.bytabit.app.core.common.file.Entity;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.payment.model.CurrencyCode;
import com.bytabit.app.core.payment.model.PaymentMethod;
import com.bytabit.app.core.wallet.model.TransactionWithAmt;

import org.bitcoinj.core.Sha256Hash;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

import static com.bytabit.app.core.offer.model.Offer.OfferType.BUY;
import static com.bytabit.app.core.offer.model.Offer.OfferType.SELL;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@ToString
public class Trade implements Entity {

    public enum Status {

        CREATED, ACCEPTED, FUNDING, FUNDED, PAID, COMPLETING, // happy path
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

    @EqualsAndHashCode.Include
    @NonNull
    private String id;

    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Long version = 0L;

    @EqualsAndHashCode.Exclude
    private Status status;

    @EqualsAndHashCode.Exclude
    private Role role;

    @NonNull
    private Date createdTimestamp;

    @NonNull
    private Offer offer;

    @NonNull
    private TradeRequest tradeRequest;

    private TradeAcceptance tradeAcceptance;

    @EqualsAndHashCode.Exclude
    transient private TransactionWithAmt fundingTransactionWithAmt;

    private PaymentRequest paymentRequest;

    private PayoutRequest payoutRequest;

    private ArbitrateRequest arbitrateRequest;

    @EqualsAndHashCode.Exclude
    transient private TransactionWithAmt payoutTransactionWithAmt;

    private CancelCompleted cancelCompleted;

    private PayoutCompleted payoutCompleted;

    // Offer

    public boolean hasOffer() {
        return offer != null;
    }

    public String getMakerProfilePubKey() {
        if (hasOffer()) {
            return offer.getMakerProfilePubKey();
        } else {
            return null;
        }
    }

    public CurrencyCode getCurrencyCode() {
        if (hasOffer()) {
            return offer.getCurrencyCode();
        } else {
            return null;
        }
    }

    public PaymentMethod getPaymentMethod() {
        if (hasOffer()) {
            return offer.getPaymentMethod();
        } else {
            return null;
        }
    }

    public BigDecimal getPrice() {
        if (hasOffer()) {
            return offer.getPrice().setScale(offer.getCurrencyCode().getScale(), RoundingMode.HALF_UP);
        } else {
            return null;
        }
    }

    // Take Offer Request

    public boolean hasTakeOfferRequest() {
        return tradeRequest != null;
    }

    public String getTakerProfilePubKey() {
        if (hasTakeOfferRequest()) {
            return tradeRequest.getTakerProfilePubKey();
        } else {
            return null;
        }
    }

    public String getTakerEscrowPubKey() {
        if (hasTakeOfferRequest()) {
            return tradeRequest.getTakerEscrowPubKey();
        } else {
            return null;
        }
    }

    public BigDecimal getBtcAmount() {
        if (hasTakeOfferRequest()) {
            return tradeRequest.getBtcAmount().setScale(8, RoundingMode.HALF_UP);
        } else {
            return null;
        }
    }

    public String getPayoutAddress() {
        if (hasPayoutRequest()) {
            return payoutRequest.getPayoutAddress();
        } else {
            return null;
        }
    }

    public BigDecimal getPaymentAmount() {
        if (hasOffer() && hasTakeOfferRequest()) {
            return tradeRequest.getPaymentAmount().setScale(offer.getCurrencyCode().getScale(), RoundingMode.HALF_UP);
        } else {
            return null;
        }
    }

    // Confirmation

    public boolean hasAcceptance() {
        return tradeAcceptance != null;
    }

    public String getMakerEscrowPubKey() {
        if (hasAcceptance()) {
            return tradeAcceptance.getMakerEscrowPubKey();
        } else {
            return null;
        }
    }

    public String getArbitratorProfilePubKey() {
        if (hasAcceptance()) {
            return tradeAcceptance.getArbitratorProfilePubKey();
        } else {
            return null;
        }
    }

    public String getSellerProfilePubKey() {

        return getProfilePubKey(SELL);
    }

    public String getBuyerProfilePubKey() {

        return getProfilePubKey(BUY);
    }

    private String getProfilePubKey(Offer.OfferType offerType) {
        if (offer.getOfferType().equals(offerType)) {
            return getMakerProfilePubKey();
        } else {
            return getTakerProfilePubKey();
        }
    }

    public String getSellerEscrowPubKey() {

        return getEscrowPubKey(SELL);
    }

    public String getBuyerEscrowPubKey() {

        return getEscrowPubKey(BUY);
    }

    private String getEscrowPubKey(Offer.OfferType offerType) {
        if (offer.getOfferType().equals(offerType)) {
            return getMakerEscrowPubKey();
        } else {
            return getTakerEscrowPubKey();
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

    public BigDecimal getTxFeePerKb() {
        if (hasTakeOfferRequest()) {
            return paymentRequest.getTxFeePerKb().setScale(8, RoundingMode.HALF_UP);
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
                .id(this.id)
                .version(this.version)
                .status(this.status)
                .role(this.role)
                .createdTimestamp(this.createdTimestamp)
                .offer(this.offer)
                .tradeRequest(this.tradeRequest)
                .tradeAcceptance(this.tradeAcceptance)
                .fundingTransactionWithAmt(this.fundingTransactionWithAmt)
                .paymentRequest(this.paymentRequest)
                .payoutRequest(this.payoutRequest)
                .arbitrateRequest(this.arbitrateRequest)
                .payoutTransactionWithAmt(this.payoutTransactionWithAmt)
                .payoutCompleted(this.payoutCompleted)
                .cancelCompleted(this.cancelCompleted);
    }

    public Sha256Hash sha256Hash() {

        return HashUtils.sha256Hash(offer.sha256Hash(), tradeRequest.sha256Hash(offer),
                hasAcceptance() ? tradeAcceptance.sha256Hash() : null,
                hasPaymentRequest() ? paymentRequest.sha256Hash() : null,
                hasPayoutRequest() ? payoutRequest.sha256Hash() : null,
                hasArbitrateRequest() ? arbitrateRequest.sha256Hash() : null,
                hasPayoutCompleted() ? payoutCompleted.sha256Hash() : null,
                hasCancelCompleted() ? cancelCompleted.sha256Hash() : null);
    }
}
