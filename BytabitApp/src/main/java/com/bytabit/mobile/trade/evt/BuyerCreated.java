package com.bytabit.mobile.trade.evt;

import com.bytabit.mobile.offer.model.SellOffer;
import com.bytabit.mobile.trade.model.BuyRequest;
import com.bytabit.mobile.trade.model.Trade;

public class BuyerCreated extends TradeEvent {

    private SellOffer sellOffer;

    private BuyRequest buyRequest;

    public BuyerCreated(String escrowAddress, Trade.Role role, SellOffer sellOffer, BuyRequest buyRequest) {
        super(escrowAddress, role);
        this.sellOffer = sellOffer;
        this.buyRequest = buyRequest;
    }

    public SellOffer getSellOffer() {
        return sellOffer;
    }

    public BuyRequest getBuyRequest() {
        return buyRequest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BuyerCreated created = (BuyerCreated) o;

        if (sellOffer != null ? !sellOffer.equals(created.sellOffer) : created.sellOffer != null)
            return false;
        return buyRequest != null ? buyRequest.equals(created.buyRequest) : created.buyRequest == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (sellOffer != null ? sellOffer.hashCode() : 0);
        result = 31 * result + (buyRequest != null ? buyRequest.hashCode() : 0);
        return result;
    }
}
