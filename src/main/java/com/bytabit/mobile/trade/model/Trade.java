package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.BuyRequest;
import com.bytabit.mobile.offer.model.SellOffer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Trade {

    public Trade() {
    }

    public Trade(SellOffer sellOffer, BuyRequest buyRequest, String escrowAddress) {
        setSellOffer(sellOffer);
        setBuyRequest(buyRequest);
        setEscrowAddress(escrowAddress);
    }

    private final StringProperty escrowAddress = new SimpleStringProperty();
    private final ObjectProperty<SellOffer> sellOffer = new SimpleObjectProperty<>();
    private final ObjectProperty<BuyRequest> buyRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentRequest> paymentRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<PayoutRequest> payoutRequest = new SimpleObjectProperty<>();
    private final StringProperty payoutTxHash = new SimpleStringProperty();

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

    public String getPayoutTxHash() {
        return payoutTxHash.get();
    }

    public StringProperty payoutTxHashProperty() {
        return payoutTxHash;
    }

    public void setPayoutTxHash(String payoutTxHash) {
        this.payoutTxHash.set(payoutTxHash);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Trade{");
        sb.append("escrowAddress=").append(escrowAddress);
        sb.append(", sellOffer=").append(sellOffer);
        sb.append(", buyRequest=").append(buyRequest);
        sb.append(", paymentRequest=").append(paymentRequest);
        sb.append(", payoutRequest=").append(payoutRequest);
        sb.append(", payoutTxHash=").append(payoutTxHash);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Trade trade = (Trade) o;

        return escrowAddress.get() != null ? escrowAddress.get().equals(trade.escrowAddress.get()) : trade.escrowAddress.get() == null;
    }

    @Override
    public int hashCode() {
        return escrowAddress.get() != null ? escrowAddress.get().hashCode() : 0;
    }
}
