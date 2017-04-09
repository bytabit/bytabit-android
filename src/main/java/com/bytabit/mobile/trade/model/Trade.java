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

    private final ObjectProperty<State> state = new SimpleObjectProperty<>();
    private final ObjectProperty<SellOffer> offer = new SimpleObjectProperty<>();
    private final ObjectProperty<BuyRequest> buyRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentRequest> sellerPaymentRequest = new SimpleObjectProperty<>();
    private final ObjectProperty<PayoutRequest> buyerPayoutRequest = new SimpleObjectProperty<>();

    private final StringProperty buyerPayoutTxId = new SimpleStringProperty();

    public State getState() {
        return state.get();
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public void setState(State state) {
        this.state.set(state);
    }

    public SellOffer getOffer() {
        return offer.get();
    }

    public ObjectProperty<SellOffer> offerProperty() {
        return offer;
    }

    public void setOffer(SellOffer offer) {
        this.offer.set(offer);
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

    public String getBuyerPayoutTxId() {
        return buyerPayoutTxId.get();
    }

    public StringProperty buyerPayoutTxIdProperty() {
        return buyerPayoutTxId;
    }

    public void setBuyerPayoutTxId(String buyerPayoutTxId) {
        this.buyerPayoutTxId.set(buyerPayoutTxId);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Trade{");
        sb.append("state=").append(state);
        sb.append(", offer=").append(offer);
        sb.append(", buyRequest=").append(buyRequest);
        sb.append(", sellerPaymentRequest=").append(sellerPaymentRequest);
        sb.append(", buyerPayoutRequest=").append(buyerPayoutRequest);
        sb.append(", buyerPayoutTxId=").append(buyerPayoutTxId);
        sb.append('}');
        return sb.toString();
    }
}
