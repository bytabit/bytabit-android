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

package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.Offer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import com.bytabit.mobile.wallet.model.TransactionWithAmt;
import lombok.*;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;

import static com.bytabit.mobile.offer.model.Offer.OfferType.BUY;
import static com.bytabit.mobile.offer.model.Offer.OfferType.SELL;
import static com.bytabit.mobile.trade.model.Trade.Role.*;
import static com.bytabit.mobile.trade.model.Trade.Status.*;

@AllArgsConstructor
@Getter
@Builder
@EqualsAndHashCode
@ToString
public class Trade {

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

    @Getter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private String id;

    @EqualsAndHashCode.Exclude
    @Builder.Default
    private final Long version = 0L;

    @EqualsAndHashCode.Exclude
    private final Status status;

    @EqualsAndHashCode.Exclude
    private final Role role;

    private final ZonedDateTime createdTimestamp;

    @NonNull
    private final Offer offer;

    @NonNull
    private final TradeRequest tradeRequest;

    private final TradeAcceptance tradeAcceptance;

    @EqualsAndHashCode.Exclude
    private TransactionWithAmt fundingTransactionWithAmt;

    private PaymentRequest paymentRequest;

    private PayoutRequest payoutRequest;

    private ArbitrateRequest arbitrateRequest;

    @EqualsAndHashCode.Exclude
    private TransactionWithAmt payoutTransactionWithAmt;

    private CancelCompleted cancelCompleted;

    private PayoutCompleted payoutCompleted;

    // Use Hex encoded Sha256 Hash of Offer.id and TakeOfferRequest properties
    public String getId() {
        if (id == null && hasOffer() && hasTakeOfferRequest()) {
            String idString = String.format("|%s|%s|%s|%s|%s|", offer.getId(),
                    tradeRequest.getTakerProfilePubKey(),
                    tradeRequest.getTakerEscrowPubKey(),
                    tradeRequest.getBtcAmount().setScale(8, RoundingMode.HALF_UP),
                    tradeRequest.getPaymentAmount().setScale(offer.getCurrencyCode().getScale(), RoundingMode.HALF_UP));

            id = Base58.encode(Sha256Hash.of(idString.getBytes()).getBytes());
        }
        return id;
    }

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

    public boolean hasConfirmation() {
        return tradeAcceptance != null;
    }

    public String getMakerEscrowPubKey() {
        if (hasConfirmation()) {
            return tradeAcceptance.getMakerEscrowPubKey();
        } else {
            return null;
        }
    }

    public String getArbitratorProfilePubKey() {
        if (hasConfirmation()) {
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

    public Trade withRole(String profilePubKey) {

        Trade tradeWithRole;

        if (SELL.equals(getOffer().getOfferType()) && getMakerProfilePubKey().equals(profilePubKey)) {
            tradeWithRole = this.copyBuilder().role(SELLER).build();
        } else if (BUY.equals(getOffer().getOfferType()) && getMakerProfilePubKey().equals(profilePubKey)) {
            tradeWithRole = this.copyBuilder().role(BUYER).build();
        } else if (SELL.equals(getOffer().getOfferType()) && getTakerProfilePubKey().equals(profilePubKey)) {
            tradeWithRole = this.copyBuilder().role(BUYER).build();
        } else if (BUY.equals(getOffer().getOfferType()) && getTakerProfilePubKey().equals(profilePubKey)) {
            tradeWithRole = this.copyBuilder().role(SELLER).build();
        } else if (hasConfirmation() && getArbitratorProfilePubKey().equals(profilePubKey)) {
            tradeWithRole = this.copyBuilder().role(ARBITRATOR).build();
        } else {
            throw new TradeManagerException("Unable to determine trade role.");
        }
        return tradeWithRole;
    }

    public Trade withStatus() {

        Trade.Status newStatus = null;
        if (hasOffer() && hasTakeOfferRequest()) {
            newStatus = CREATED;
        }
        if (newStatus == CREATED && hasConfirmation()) {
            newStatus = ACCEPTED;
        }
        if (newStatus == ACCEPTED && hasPaymentRequest()) {
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
        if ((newStatus == CREATED || newStatus == ACCEPTED) && hasCancelCompleted() &&
                (getCancelCompleted().getReason().equals(CancelCompleted.Reason.SELLER_CANCEL_UNFUNDED) ||
                        getCancelCompleted().getReason().equals(CancelCompleted.Reason.BUYER_CANCEL_UNFUNDED))) {
            newStatus = CANCELED;
        }
        if ((newStatus == FUNDING || newStatus == FUNDED) && hasCancelCompleted() &&
                getCancelCompleted().getReason().equals(CancelCompleted.Reason.BUYER_CANCEL_FUNDED)) {
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
