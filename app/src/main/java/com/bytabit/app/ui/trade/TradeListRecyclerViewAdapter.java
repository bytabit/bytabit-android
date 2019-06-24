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

package com.bytabit.app.ui.trade;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bytabit.app.R;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.core.trade.model.TradeRequest;

import java.util.ArrayList;
import java.util.List;

import static com.bytabit.app.core.offer.model.Offer.OfferType.BUY;
import static com.bytabit.app.core.offer.model.Offer.OfferType.SELL;
import static com.bytabit.app.ui.trade.TradeListFragment.OnListFragmentInteractionListener;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Trade} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class TradeListRecyclerViewAdapter extends RecyclerView.Adapter<TradeListRecyclerViewAdapter.TradeViewHolder> {

    private final List<Trade> trades = new ArrayList<>();

    private final OnListFragmentInteractionListener interactionListener;


    public TradeListRecyclerViewAdapter(OnListFragmentInteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    public void setTrades(List<Trade> trades) {
        this.trades.clear();
        this.trades.addAll(trades);
        notifyDataSetChanged();
    }

    public void addOrUpdateTrade(Trade trade) {

        boolean found = false;
        for (int index = 0; index < this.trades.size(); index++) {
            Trade existingTrade = this.trades.get(index);
            if (existingTrade.getId().equals(trade.getId())) {
                this.trades.set(index, trade);
                notifyItemChanged(index);
                found = true;
                break;
            }
        }
        if (!found) {
            this.trades.add(trade);
            notifyItemInserted(this.trades.size() - 1);
        }
    }

    @Override
    public TradeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_trade_item, parent, false);
        return new TradeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final TradeViewHolder holder, int position) {

        Trade trade = trades.get(position);
        holder.trade = trade;

        Offer offer = trade.getOffer();
        TradeRequest tradeRequest = trade.getTradeRequest();

        Offer.OfferType offerType = trade.getOffer().getOfferType();

        if (!trade.getOffer().getIsMine()) {
            // swap trade type if not my trade
            offerType = SELL.equals(trade.getOffer().getOfferType()) ? BUY : SELL;
        }

        String status = trade.getStatus().toString();
        String amount = String.format("%s %s BTC for %s %s", offerType.toString(), tradeRequest.getBtcAmount().toPlainString(), tradeRequest.getPaymentAmount().toPlainString(), offer.getCurrencyCode().toString());
        String details = String.format("%s %s/BTC via %s", offer.getPrice().toPlainString(), offer.getCurrencyCode().toString(), offer.getPaymentMethod().displayName());

        holder.offerStatusLabel.setText(status);
        holder.offerItemAmountView.setText(amount);
        holder.offerItemDetailsView.setText(details);

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != interactionListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    interactionListener.onListFragmentInteraction(holder.trade);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return trades.size();
    }

    class TradeViewHolder extends RecyclerView.ViewHolder {

        final View view;
        final TextView offerStatusLabel;
        final TextView offerItemAmountView;
        final TextView offerItemDetailsView;
        Trade trade;

        TradeViewHolder(View view) {
            super(view);
            this.view = view;
            offerStatusLabel = view.findViewById(R.id.trade_item_status);
            offerItemAmountView = view.findViewById(R.id.trade_item_amount);
            offerItemDetailsView = view.findViewById(R.id.trade_item_details);
        }
    }
}
