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

package com.bytabit.app.ui.offer;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bytabit.app.R;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.ui.offer.OfferListFragment.OnListFragmentInteractionListener;

import java.util.ArrayList;
import java.util.List;

import static com.bytabit.app.core.offer.model.Offer.OfferType.BUY;
import static com.bytabit.app.core.offer.model.Offer.OfferType.SELL;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Offer} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class OffersRecyclerViewAdapter extends RecyclerView.Adapter<OffersRecyclerViewAdapter.OfferViewHolder> {

    private final List<Offer> offers = new ArrayList<>();

    private final OnListFragmentInteractionListener interactionListener;


    public OffersRecyclerViewAdapter(OnListFragmentInteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    public void setOffers(List<Offer> offers) {
        this.offers.clear();
        this.offers.addAll(offers);
        notifyDataSetChanged();
    }

    @Override
    public OfferViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_offer_item, parent, false);
        return new OfferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final OfferViewHolder holder, int position) {

        Offer offer = offers.get(position);
        holder.offer = offer;

        Offer.OfferType offerType = offer.getOfferType();

        if (offer.getIsMine()) {
            // use different style if my offer
            // TODO tile.getStyleClass().add("my-offer");
        } else {
            // swap offer type if not my offer
            offerType = SELL.equals(offer.getOfferType()) ? BUY : SELL;
        }

        String amount = String.format("%s @ %s %s per BTC", offerType.toString(), offer.getPrice().toPlainString(), offer.getCurrencyCode().toString());
        String details = String.format("%s to %s %s via %s", offer.getMinAmount(), offer.getMaxAmount(), offer.getCurrencyCode(), offer.getPaymentMethod().displayName());

        holder.offerItemAmountView.setText(amount);
        holder.offerItemDetailsView.setText(details);
        if (offer.getIsMine()) {
            holder.offerIsMineLabel.setVisibility(View.VISIBLE);
        } else {
            holder.offerIsMineLabel.setVisibility(View.GONE);
        }

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != interactionListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    interactionListener.onListFragmentInteraction(holder.offer);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return offers.size();
    }

    class OfferViewHolder extends RecyclerView.ViewHolder {

        final View view;
        final TextView offerItemAmountView;
        final TextView offerItemDetailsView;
        final TextView offerIsMineLabel;
        Offer offer;

        OfferViewHolder(View view) {
            super(view);
            this.view = view;
            offerItemAmountView = view.findViewById(R.id.offer_item_amount);
            offerItemDetailsView = view.findViewById(R.id.offer_item_details);
            offerIsMineLabel = view.findViewById(R.id.offer_is_mine_label);
        }
    }
}
