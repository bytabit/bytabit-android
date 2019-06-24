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

package com.bytabit.app.ui.wallet;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bytabit.app.R;
import com.bytabit.app.core.wallet.model.TransactionWithAmt;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@link RecyclerView.Adapter} that can display a {@link TransactionWithAmt}.
 */
public class WalletRecyclerViewAdapter extends RecyclerView.Adapter<WalletRecyclerViewAdapter.TransactionViewHolder> {

    private final List<TransactionWithAmt> transactions = new ArrayList<>();

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    public void updateTransaction(TransactionWithAmt transactionWithAmt) {
        int index = transactions.indexOf(transactionWithAmt);
        if (index > -1) {
            transactions.set(index, transactionWithAmt);
        } else {
            transactions.add(transactionWithAmt);
        }
        Collections.sort(transactions, (tx1, tx2) -> tx1.getDepth().compareTo(tx2.getDepth()));
        notifyDataSetChanged();
    }

    @Override
    public TransactionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_transaction_item, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final TransactionViewHolder holder, int position) {

        TransactionWithAmt transaction = transactions.get(position);
        holder.transaction = transaction;

        String amount = String.format("%s, %s", dateFormat.format(transaction.getDate()), transaction.getTransactionAmt().toFriendlyString());
        String details = String.format(Locale.US, "%s (%d), Hash: %s", transaction.getConfidenceType(), transaction.getDepth(), transaction.getHash().substring(0, 16));

        holder.transactionItemAmountView.setText(amount);
        holder.transactionItemDetailsView.setText(details);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        final View view;
        final TextView transactionItemAmountView;
        final TextView transactionItemDetailsView;
        TransactionWithAmt transaction;

        TransactionViewHolder(View view) {
            super(view);
            this.view = view;
            transactionItemAmountView = view.findViewById(R.id.transaction_item_amount);
            transactionItemDetailsView = view.findViewById(R.id.transaction_item_details);
        }
    }
}
