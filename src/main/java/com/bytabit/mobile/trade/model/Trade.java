package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.SellOffer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import static com.bytabit.mobile.trade.model.Trade.Role.*;
import static com.bytabit.mobile.trade.model.Trade.Status.*;

public class Trade {

    public enum Status {
        CREATED, FUNDED, PAID, COMPLETED, ARBITRATING
    }

    public enum Role {
        BUYER, SELLER, ARBITRATOR
    }

    public Trade() {
    }

    private final ObjectProperty<Status> status = new SimpleObjectProperty<>();
    private final StringProperty escrowAddress = new SimpleStringProperty();
    private final ObjectProperty<SellOffer> sellOffer = new SimpleObjectProperty<>();
    private final ObjectProperty<BuyRequest> buyRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentRequest> paymentRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<PayoutRequest> payoutRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<ArbitrateRequest> arbitrateRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<PayoutCompleted> payoutCompleted = new SimpleObjectProperty<>();

    public Trade(SellOffer sellOffer, BuyRequest buyRequest, String escrowAddress) {
        this.sellOffer.set(sellOffer);
        this.buyRequest.set(buyRequest);
        this.escrowAddress.set(escrowAddress);
        this.status.set(CREATED);
    }

    public void setSellOffer(SellOffer sellOffer) {
        if (sellOffer != null && getBuyRequest() == null
                && getPaymentRequest() == null && getPayoutRequest() == null
                && getPayoutCompleted() == null) {
            this.sellOffer.setValue(sellOffer);
            this.status.setValue(CREATED);
        } else {
            throw new RuntimeException("Invalid trade status.");
        }
    }

    public void setBuyRequest(BuyRequest buyRequest) {
        if (getSellOffer() != null && buyRequest != null
                && getPaymentRequest() == null && getPayoutRequest() == null
                && getPayoutCompleted() == null) {
            this.escrowAddress.setValue(buyRequest.getEscrowAddress());
            this.buyRequest.setValue(buyRequest);
            this.status.setValue(CREATED);
        } else {
            throw new RuntimeException("Invalid trade status.");
        }
    }

    public void setPaymentRequest(PaymentRequest paymentRequest) {
        if (getSellOffer() != null && getBuyRequest() != null
                && paymentRequest != null && getPayoutRequest() == null
                && getPayoutCompleted() == null) {
            this.paymentRequest.set(paymentRequest);
            this.status.setValue(FUNDED);
        }
    }

    public void setPayoutRequest(PayoutRequest payoutRequest) {
        if (getSellOffer() != null && getBuyRequest() != null
                && getPaymentRequest() != null && payoutRequest != null
                && getPayoutCompleted() == null) {
            this.payoutRequest.set(payoutRequest);
            this.status.setValue(PAID);
        }
    }

    public void setArbitrateRequest(ArbitrateRequest arbitrateRequest) {
        if (getSellOffer() != null && getBuyRequest() != null
                && getPaymentRequest() != null
                && getPayoutCompleted() == null && arbitrateRequest != null) {
            this.arbitrateRequest.set(arbitrateRequest);
            this.status.setValue(ARBITRATING);
        }
    }

    public void setPayoutCompleted(PayoutCompleted payoutCompleted) {
        if (getSellOffer() != null && getBuyRequest() != null
                && getPaymentRequest() != null && (getPayoutRequest() != null || getArbitrateRequest() != null)
                && payoutCompleted != null) {
            this.payoutCompleted.set(payoutCompleted);
            this.status.setValue(COMPLETED);
        }
    }

    public Status getStatus() {
        return status.get();
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    public String getEscrowAddress() {
        return escrowAddress.get();
    }

    public StringProperty escrowAddressProperty() {
        return escrowAddress;
    }

    public SellOffer getSellOffer() {
        return sellOffer.get();
    }

    public ObjectProperty<SellOffer> sellOfferProperty() {
        return sellOffer;
    }

    public BuyRequest getBuyRequest() {
        return buyRequest.get();
    }

    public ObjectProperty<BuyRequest> buyRequestProperty() {
        return buyRequest;
    }

    public PaymentRequest getPaymentRequest() {
        return paymentRequest.get();
    }

    public ObjectProperty<PaymentRequest> paymentRequestProperty() {
        return paymentRequest;
    }

    public PayoutRequest getPayoutRequest() {
        return payoutRequest.get();
    }

    public ObjectProperty<PayoutRequest> payoutRequestProperty() {
        return payoutRequest;
    }

    public PayoutCompleted getPayoutCompleted() {
        return payoutCompleted.get();
    }

    public ObjectProperty<PayoutCompleted> payoutCompletedProperty() {
        return payoutCompleted;
    }

    public ArbitrateRequest getArbitrateRequest() {
        return arbitrateRequest.get();
    }

    public ObjectProperty<ArbitrateRequest> arbitrateRequestProperty() {
        return arbitrateRequest;
    }

    public Role getRole(String profilePubKey, Boolean isArbitrator) {
        Role role;

        if (!isArbitrator) {
            if (getSellOffer().getSellerProfilePubKey().equals(profilePubKey)) {
                role = SELLER;
            } else if (getBuyRequest().getBuyerProfilePubKey().equals(profilePubKey)) {
                role = BUYER;
            } else {
                throw new RuntimeException("Unable to determine trader role.");
            }
        } else if (getSellOffer().getArbitratorProfilePubKey().equals(profilePubKey)) {
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

        return escrowAddress != null ? escrowAddress.get().equals(trade.escrowAddress.get()) : trade.escrowAddress == null;
    }

    @Override
    public int hashCode() {
        return escrowAddress != null ? escrowAddress.hashCode() : 0;
    }
}
