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

import android.content.Context;
import android.os.Bundle;
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
import com.bytabit.app.core.offer.manager.OfferManager;
import com.bytabit.app.core.trade.manager.TradeManager;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.core.wallet.manager.WalletManager;
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
public class TradeListFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener interactionListener;

    private CompositeDisposable compositeDisposable;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TradeListFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static TradeListFragment newInstance(int columnCount) {
        TradeListFragment fragment = new TradeListFragment();
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

        compositeDisposable = new CompositeDisposable();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trade_list, container, false);

        Single<ApplicationComponent> applicationComponent = ((BytabitApplication) view.getContext()
                .getApplicationContext()).getApplicationComponent();

        Single<TradeManager> tradeManager = applicationComponent
                .map(ApplicationComponent::tradeManager);

        Single<OfferManager> offerManager = applicationComponent
                .map(ApplicationComponent::offerManager);

        Single<WalletManager> walletManager = applicationComponent
                .map(ApplicationComponent::walletManager);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            TradeListRecyclerViewAdapter tradeListViewAdapter = new TradeListRecyclerViewAdapter(interactionListener);

            // setup event observables

            Single<String> profilePubKeyBase58 = walletManager.flatMapMaybe(WalletManager::getProfilePubKeyBase58)
                    .toSingle().cache();

            Disposable storedTradesDisposable = tradeManager
                    .flatMap(TradeManager::getStoredTrades)
                    .flattenAsObservable(tl -> tl)
                    .flatMapSingle(t -> profilePubKeyBase58.map(pubKey -> {
                        t.getOffer().setIsMine(t.getOffer().getMakerProfilePubKey().equals(pubKey));
                        return t;
                    }))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(tradeListViewAdapter::addOrUpdateTrade);

            compositeDisposable.add(storedTradesDisposable);

            View tradeListView = view.findViewById(R.id.trade_list);

            Disposable walletSyncedDisposable = walletManager
                    .flatMapObservable(WalletManager::getWalletSynced)
                    .startWith(false)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(tradeListView::setEnabled);

            compositeDisposable.add(walletSyncedDisposable);

            Disposable updatedTradesDisposable = tradeManager
                    .flatMapObservable(TradeManager::getUpdatedTrades)
                    .mergeWith(offerManager.flatMapObservable(OfferManager::getAddedTrades))
                    .flatMapSingle(t -> profilePubKeyBase58.map(pubKey -> {
                        t.getOffer().setIsMine(t.getOffer().getMakerProfilePubKey().equals(pubKey));
                        return t;
                    }))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(tradeListViewAdapter::addOrUpdateTrade);

            compositeDisposable.add(updatedTradesDisposable);

            recyclerView.setAdapter(tradeListViewAdapter);
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
        mainActivity.setActionBarTitle(R.string.menu_trade_list);

        // setup fab
        mainActivity.hideAddFab();
        mainActivity.hideRemoveFab();
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
        void onListFragmentInteraction(Trade trade);
    }
}
