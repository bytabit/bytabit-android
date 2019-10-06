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

package com.bytabit.app.ui.badge;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bytabit.app.R;
import com.bytabit.app.core.badge.BadgeException;
import com.bytabit.app.core.badge.model.Badge;
import com.bytabit.app.core.payment.model.PaymentDetails;
import com.bytabit.app.ui.payment.PaymentListFragment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PaymentDetails} and makes a call to the
 * specified {@link PaymentListFragment.OnListFragmentInteractionListener}.
 */
public class BadgesRecyclerViewAdapter extends RecyclerView.Adapter<BadgesRecyclerViewAdapter.BadgeViewHolder> {

    private final List<Badge> badges = new ArrayList<>();

    private final BadgeListFragment.OnListFragmentInteractionListener interactionListener;

    private final DateFormat dateFormat;

    public BadgesRecyclerViewAdapter(BadgeListFragment.OnListFragmentInteractionListener interactionListener) {

        this.interactionListener = interactionListener;

        dateFormat = new SimpleDateFormat("yyyy-MMM-dd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
    }

    public void addOrUpdateBadge(Badge badge) {

        boolean found = false;
        for (int index = 0; index < this.badges.size(); index++) {
            Badge existingBadge = this.badges.get(index);
            if (existingBadge.getId().equals(badge.getId())) {
                this.badges.set(index, badge);
                notifyItemChanged(index);
                found = true;
                break;
            }
        }
        if (!found) {
            this.badges.add(badge);
            notifyItemInserted(this.badges.size() - 1);
        }
    }

    public void removeBadge(Badge badge) {
        if (this.badges.contains(badge)) {
            int index = this.badges.indexOf(badge);
            this.badges.remove(index);
            notifyItemRemoved(index);
        }
    }

    @Override
    public BadgeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_badge_item, parent, false);

        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final BadgeViewHolder holder, int position) {

        Badge badge = badges.get(position);

        String type = "";

        switch (badge.getBadgeType()) {
            case BETA_TESTER:
                type = String.format("%s", badge.getBadgeType().toString());
                break;
            case OFFER_MAKER:
                type = String.format("%s for %s", badge.getBadgeType().toString(), badge.getCurrencyCode().toString());
                break;
            case DETAILS_VERIFIED:
                type = String.format("%s for %s via %s", badge.getBadgeType().toString(), badge.getCurrencyCode().toString(), badge.getPaymentMethod().toString());
                break;
            default:
                throw new BadgeException("Invalid badge type.");
        }

        String valid = String.format("valid from %s%s", dateFormat.format(badge.getValidFrom()),
                badge.getValidTo() != null ? " to " + dateFormat.format(badge.getValidTo()) : "");

        holder.badge = badge;
        holder.badgeItemTypeView.setText(type);
        holder.badgeItemValidView.setText(valid);

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != interactionListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    interactionListener.onListFragmentInteraction(holder.badge);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    class BadgeViewHolder extends RecyclerView.ViewHolder {

        final View view;
        final TextView badgeItemTypeView;
        final TextView badgeItemValidView;
        Badge badge;

        BadgeViewHolder(View view) {
            super(view);
            this.view = view;
            badgeItemTypeView = view.findViewById(R.id.badge_item_type);
            badgeItemValidView = view.findViewById(R.id.badge_item_valid);
        }
    }
}
