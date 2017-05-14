package com.bytabit.mobile.trade.model;

import com.bytabit.mobile.offer.model.BuyRequest;
import com.bytabit.mobile.offer.model.SellOffer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Trade {

    public enum State {
        BUY_REQUESTED, ESCROW_FUNDED,
        PAYMENT_REQUESTED, PAYMENT_SENT,
        PAYMENT_RECEIVED, BUYER_PAYOUT, SELLER_REFUND,
        ARBITRATION_REQUESTED, CANCEL_REQUESTED
    }

    public Trade() {
    }

    public Trade(SellOffer sellOffer, BuyRequest buyRequest, State state, String escrowAddress) {
        setSellOffer(sellOffer);
        setBuyRequest(buyRequest);
        setState(state);
        setEscrowAddress(escrowAddress);
    }

    private final StringProperty escrowAddress = new SimpleStringProperty();
    private final ObjectProperty<State> state = new SimpleObjectProperty<>();
    private final ObjectProperty<SellOffer> sellOffer = new SimpleObjectProperty<>();
    private final ObjectProperty<BuyRequest> buyRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentRequest> sellerPaymentRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<PayoutRequest> buyerPayoutRequest = new SimpleObjectProperty<>();
    private final StringProperty buyerPayoutTxHash = new SimpleStringProperty();

    public String getEscrowAddress() {
        return escrowAddress.get();
    }

    public StringProperty escrowAddressProperty() {
        return escrowAddress;
    }

    public void setEscrowAddress(String escrowAddress) {
        this.escrowAddress.set(escrowAddress);
    }

    public State getState() {
        return state.get();
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public void setState(State state) {
        this.state.set(state);
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

    public PaymentRequest getSellerPaymentRequest() {
        return sellerPaymentRequest.get();
    }

    public ObjectProperty<PaymentRequest> sellerPaymentRequestProperty() {
        return sellerPaymentRequest;
    }

    public void setSellerPaymentRequest(PaymentRequest sellerPaymentRequest) {
        this.sellerPaymentRequest.set(sellerPaymentRequest);
    }

    public PayoutRequest getBuyerPayoutRequest() {
        return buyerPayoutRequest.get();
    }

    public ObjectProperty<PayoutRequest> buyerPayoutRequestProperty() {
        return buyerPayoutRequest;
    }

    public void setBuyerPayoutRequest(PayoutRequest buyerPayoutRequest) {
        this.buyerPayoutRequest.set(buyerPayoutRequest);
    }

    public String getBuyerPayoutTxHash() {
        return buyerPayoutTxHash.get();
    }

    public StringProperty buyerPayoutTxHashProperty() {
        return buyerPayoutTxHash;
    }

    public void setBuyerPayoutTxHash(String buyerPayoutTxHash) {
        this.buyerPayoutTxHash.set(buyerPayoutTxHash);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Trade{");
        sb.append("state=").append(state);
        sb.append(", sellOffer=").append(sellOffer);
        sb.append(", buyRequest=").append(buyRequest);
        sb.append(", sellerPaymentRequest=").append(sellerPaymentRequest);
        sb.append(", buyerPayoutRequest=").append(buyerPayoutRequest);
        sb.append(", buyerPayoutTxHash=").append(buyerPayoutTxHash);
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
