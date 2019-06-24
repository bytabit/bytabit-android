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

package com.bytabit.app.ui.payment;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bytabit.app.R;
import com.bytabit.app.core.payment.model.PaymentDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PaymentDetails} and makes a call to the
 * specified {@link PaymentListFragment.OnListFragmentInteractionListener}.
 */
public class PaymentsRecyclerViewAdapter extends RecyclerView.Adapter<PaymentsRecyclerViewAdapter.PaymentViewHolder> {

    private final List<PaymentDetails> payments = new ArrayList<>();

    private final PaymentListFragment.OnListFragmentInteractionListener interactionListener;

    public PaymentsRecyclerViewAdapter(PaymentListFragment.OnListFragmentInteractionListener interactionListener) {

        this.interactionListener = interactionListener;
    }

    public void addOrUpdatePaymentDetails(PaymentDetails paymentDetails) {

        boolean found = false;
        for (int index = 0; index < this.payments.size(); index++) {
            PaymentDetails existingPaymentDetails = this.payments.get(index);
            if (existingPaymentDetails.getId().equals(paymentDetails.getId())) {
                this.payments.set(index, paymentDetails);
                notifyItemChanged(index);
                found = true;
                break;
            }
        }
        if (!found) {
            this.payments.add(paymentDetails);
            notifyItemInserted(this.payments.size() - 1);
        }
    }

    public void removePaymentDetails(PaymentDetails paymentDetails) {
        if (this.payments.contains(paymentDetails)) {
            int index = this.payments.indexOf(paymentDetails);
            this.payments.remove(index);
            notifyItemRemoved(index);
        }
    }

    @Override
    public PaymentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_payment_item, parent, false);

        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final PaymentViewHolder holder, int position) {

        PaymentDetails paymentDetails = payments.get(position);
        holder.paymentDetails = paymentDetails;

        String currency = String.format("%s via %s", paymentDetails.getCurrencyCode().toString(), paymentDetails.getPaymentMethod());
        String details = paymentDetails.getDetails();

        holder.paymentItemCurrencyView.setText(currency);
        holder.paymentItemDetailsView.setText(details);

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != interactionListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    interactionListener.onListFragmentInteraction(holder.paymentDetails);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    class PaymentViewHolder extends RecyclerView.ViewHolder {

        final View view;
        final TextView paymentItemCurrencyView;
        final TextView paymentItemDetailsView;
        PaymentDetails paymentDetails;

        PaymentViewHolder(View view) {
            super(view);
            this.view = view;
            paymentItemCurrencyView = view.findViewById(R.id.payment_item_currency);
            paymentItemDetailsView = view.findViewById(R.id.payment_item_details);
        }
    }
}
