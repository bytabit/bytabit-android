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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytabit.app.ApplicationComponent;
import com.bytabit.app.R;
import com.bytabit.app.core.badge.BadgeManager;
import com.bytabit.app.core.badge.model.Badge;
import com.bytabit.app.core.wallet.WalletManager;
import com.bytabit.app.ui.BytabitApplication;
import com.bytabit.app.ui.MainActivity;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class BadgeListFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener interactionListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BadgeListFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static BadgeListFragment newInstance(int columnCount) {
        BadgeListFragment fragment = new BadgeListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            interactionListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_badge_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            BadgesRecyclerViewAdapter badgesViewAdapter = new BadgesRecyclerViewAdapter(interactionListener);

            // get stored badges
            Disposable badgesDisposable = ((BytabitApplication) view.getContext().getApplicationContext())
                    .getApplicationComponent().map(ApplicationComponent::badgeManager)
                    .flatMap(BadgeManager::getStoredBadges)
                    .flattenAsObservable(b -> b)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(badgesViewAdapter::addOrUpdateBadge);

            compositeDisposable.add(badgesDisposable);

            recyclerView.setAdapter(badgesViewAdapter);
        }
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity mainActivity = ((MainActivity) getActivity());

        // set title bar
        mainActivity.setActionBarTitle(R.string.menu_badge_list);

        // setup fab
        mainActivity.hideAddFab();
        mainActivity.hideRemoveFab();

        Single<WalletManager> walletManager = ((BytabitApplication) getContext().getApplicationContext())
                .getApplicationComponent().map(ApplicationComponent::walletManager);

        Disposable walletsSynced = walletManager
                .flatMapObservable(WalletManager::getWalletSynced)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(synced -> {

                    if (synced) {
                        // setup fab
                        mainActivity.showAddFab(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Snackbar.make(view, "Add Badge", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();

                                BadgeBuyFragment badgeBuyFragment = ((MainActivity) view.getContext()).getBadgeBuyFragment();
                                getFragmentManager().beginTransaction().addToBackStack("main")
                                        .replace(R.id.content_main, badgeBuyFragment)
                                        .commit();
                            }
                        });
                    } else {
                        mainActivity.hideAddFab();
                    }

                });

        compositeDisposable.add(walletsSynced);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compositeDisposable.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        interactionListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(Badge badge);
    }
}
