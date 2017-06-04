package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.BuyRequest;
import com.bytabit.mobile.offer.model.SellOffer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import static com.bytabit.mobile.trade.model.Trade.Role.*;
import static com.bytabit.mobile.trade.model.Trade.Status.*;

public class Trade {

    public enum Status {
        CREATED, FUNDED, PAID, COMPLETED
    }

    public enum Role {
        BUYER, SELLER, ARBITRATOR, UNKNOWN
    }

    public Trade() {
    }

    public Trade(SellOffer sellOffer, BuyRequest buyRequest, String escrowAddress) {
        setSellOffer(sellOffer);
        setBuyRequest(buyRequest);
        setEscrowAddress(escrowAddress);
        setStatus(CREATED);

        this.sellOffer.addListener((e) -> updateStatus());
        this.buyRequest.addListener((e) -> updateStatus());
        this.paymentRequest.addListener((e) -> updateStatus());
        this.payoutRequest.addListener((e) -> updateStatus());
        this.payoutDetails.addListener((e) -> updateStatus());
    }

    private final ObjectProperty<Status> status = new SimpleObjectProperty<>();
    private final StringProperty escrowAddress = new SimpleStringProperty();
    private final ObjectProperty<SellOffer> sellOffer = new SimpleObjectProperty<>();
    private final ObjectProperty<BuyRequest> buyRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentRequest> paymentRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<PayoutRequest> payoutRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<PayoutDetails> payoutDetails = new SimpleObjectProperty<>();

    public Status getStatus() {
        return status.get();
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    public void setStatus(Status status) {
        this.status.set(status);
    }

    public String getEscrowAddress() {
        return escrowAddress.get();
    }

    public StringProperty escrowAddressProperty() {
        return escrowAddress;
    }

    public void setEscrowAddress(String escrowAddress) {
        this.escrowAddress.set(escrowAddress);
    }

    public SellOffer getSellOffer() {
        return sellOffer.get();
    }

    public ObjectProperty<SellOffer> sellOfferProperty() {
        return sellOffer;
    }

    public void setSellOffer(SellOffer sellOffer) {
        this.sellOffer.set(sellOffer);
    }

    public BuyRequest getBuyRequest() {
        return buyRequest.get();
    }

    public ObjectProperty<BuyRequest> buyRequestProperty() {
        return buyRequest;
    }

    public void setBuyRequest(BuyRequest buyRequest) {
        this.buyRequest.set(buyRequest);
    }

    public PaymentRequest getPaymentRequest() {
        return paymentRequest.get();
    }

    public ObjectProperty<PaymentRequest> paymentRequestProperty() {
        return paymentRequest;
    }

    public void setPaymentRequest(PaymentRequest paymentRequest) {
        this.paymentRequest.set(paymentRequest);
    }

    public PayoutRequest getPayoutRequest() {
        return payoutRequest.get();
    }

    public ObjectProperty<PayoutRequest> payoutRequestProperty() {
        return payoutRequest;
    }

    public void setPayoutRequest(PayoutRequest payoutRequest) {
        this.payoutRequest.set(payoutRequest);
    }

    public PayoutDetails getPayoutDetails() {
        return payoutDetails.get();
    }

    public ObjectProperty<PayoutDetails> payoutDetailsProperty() {
        return payoutDetails;
    }

    public void setPayoutDetails(PayoutDetails payoutDetails) {
        this.payoutDetails.set(payoutDetails);
    }

    private void updateStatus() {

        // created: SellOffer + BuyRequest
        if (getSellOffer() != null && getBuyRequest() != null
                && getPaymentRequest() == null && getPayoutRequest() == null
                && getPayoutDetails() == null) {
            setStatus(CREATED);
        }
        // funded: fundEscrow + PaymentRequest
        else if (getSellOffer() != null && getBuyRequest() != null
                && getPaymentRequest() != null && getPayoutRequest() == null
                && getPayoutDetails() == null) {
            setStatus(FUNDED);
        }
        // paid: PayoutRequest + payoutEscrow
        else if (getSellOffer() != null && getBuyRequest() != null
                && getPaymentRequest() != null && getPayoutRequest() != null
                && getPayoutDetails() == null) {
            setStatus(PAID);
        }
        // complete: payoutDetails
        else if (getSellOffer() != null && getBuyRequest() != null
                && getPaymentRequest() != null && getPayoutRequest() != null
                && getPayoutDetails() != null) {
            setStatus(COMPLETED);
        } else {
            throw new RuntimeException("Invalid trade status.");
        }
    }

    private Role getRole(String profilePubKey) {
        Role role;

        if (getSellOffer().getSellerProfilePubKey().equals(profilePubKey)) {
            role = SELLER;
        } else if (getBuyRequest().getBuyerProfilePubKey().equals(profilePubKey)) {
            role = BUYER;
        } else if (getSellOffer().getArbitratorProfilePubKey().equals(profilePubKey)) {
            role = ARBITRATOR;
        } else {
            throw new RuntimeException("Unable to determine trade role.");
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
