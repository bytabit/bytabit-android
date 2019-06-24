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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bytabit.app.ApplicationComponent;
import com.bytabit.app.R;
import com.bytabit.app.core.wallet.manager.WalletManager;
import com.bytabit.app.ui.BytabitApplication;
import com.bytabit.app.ui.MainActivity;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

/**
 * A fragment representing a list of wallet transactions.
 */
@Slf4j
public class WalletFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;

    private Single<WalletManager> walletManager;

    private CompositeDisposable compositeDisposable;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WalletFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static WalletFragment newInstance(int columnCount) {
        WalletFragment fragment = new WalletFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        // Set the adapter

        if (view instanceof LinearLayout) {
            Context context = view.getContext();
            RecyclerView recyclerView = view.findViewById(R.id.transactions_list);
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            WalletRecyclerViewAdapter walletViewAdapter = new WalletRecyclerViewAdapter();

            walletManager = ((BytabitApplication) getContext().getApplicationContext())
                    .getApplicationComponent().map(ApplicationComponent::walletManager).cache();

            // update transactions

            Disposable walletsDownloadProgressDisposable = walletManager
                    .flatMapObservable(WalletManager::getWalletsDownloadProgress)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(progress -> {
                        ((ProgressBar) view.findViewById(R.id.wallet_progress)).setProgress(progress.intValue());
                    });

            compositeDisposable.add(walletsDownloadProgressDisposable);

            Disposable tradeWalletDisposable = walletManager
                    .flatMapObservable(WalletManager::getTradeUpdatedWalletTx)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(transaction -> {
                        walletViewAdapter.updateTransaction(transaction);
                        ((TextView) view.findViewById(R.id.wallet_balance)).setText(transaction.getWalletBalance().toFriendlyString());
                    });

            compositeDisposable.add(tradeWalletDisposable);

            recyclerView.setAdapter(walletViewAdapter);
        }
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_wallet, menu);
        MenuItem restoreButtonItem = menu.findItem(R.id.wallet_menu_restore_button);

        WalletRestoreFragment walletRestoreFragment = ((MainActivity) getContext()).getWalletRestoreFragment();

        restoreButtonItem.setOnMenuItemClickListener(item -> {
            getFragmentManager().beginTransaction().addToBackStack("main")
                    .replace(R.id.content_main, walletRestoreFragment)
                    .commit();
            return true;
        });

        MenuItem infoButtonItem = menu.findItem(R.id.wallet_menu_info_button);

        Observable<MenuItem> infoButtonObservable = Observable.create(source -> {
            infoButtonItem.setOnMenuItemClickListener(value -> {
                source.onNext(value);
                return true;
            });
        });

        compositeDisposable.add(infoButtonObservable
                .observeOn(Schedulers.io())
                .flatMapMaybe(r -> walletManager.flatMapMaybe(WalletManager::getTradeWalletInfo))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(tradeWalletInfo -> {
                    String walletInfoHeader = getContext().getString(R.string.wallet_info_header);
                    String walletSeedWords = getContext().getString(R.string.wallet_info_seed_words);
                    String walletProfilePubKey = getContext().getString(R.string.wallet_info_profile_pubkey);

                    AlertDialog.Builder alert = new AlertDialog.Builder(this.getContext());
                    alert.setTitle(walletInfoHeader);
                    alert.setMessage(String.format("%s: %s\n\n%s: %s", walletSeedWords, tradeWalletInfo.getSeedWords(),
                            walletProfilePubKey, tradeWalletInfo.getProfilePubKey()));

                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // do nothing
                        }
                    });
                    alert.show();
                    log.debug("Profile PubKey: {}", tradeWalletInfo.getProfilePubKey());
                }));

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity mainActivity = ((MainActivity) getActivity());

        // set title bar
        mainActivity.setActionBarTitle(R.string.menu_wallet);

        Single<WalletManager> walletManager = ((BytabitApplication) getContext().getApplicationContext())
                .getApplicationComponent().map(ApplicationComponent::walletManager);

        mainActivity.hideAddFab();
        mainActivity.hideRemoveFab();

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
                                Snackbar.make(view, "Deposit BTC", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();

                                WalletDepositFragment walletDepositFragment = ((MainActivity) getContext()).getWalletDepositFragment();

                                getFragmentManager().beginTransaction().addToBackStack("main")
                                        .replace(R.id.content_main, walletDepositFragment)
                                        .commit();
                            }
                        });

                        mainActivity.showRemoveFab(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Snackbar.make(view, "Withdraw BTC", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        });
                    } else {
                        mainActivity.hideAddFab();
                        mainActivity.hideRemoveFab();
                    }

                });

        compositeDisposable.add(walletsSynced);

        Disposable walletsStarted = walletManager
                .flatMapObservable(WalletManager::getWalletsRunning)
                .startWithArray(false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(running -> {
                    if (running) {
                        Snackbar.make(getView(), "Wallet started", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    } else {
                        Snackbar.make(getView(), "Wallet starting...", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                });

        compositeDisposable.add(walletsStarted);
    }

    private void showError(Throwable t) {
        log.error("wallet error", t);
        AlertDialog.Builder alert = new AlertDialog.Builder(this.getContext());
        alert.setTitle(t.getMessage());

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // do nothing
            }
        });

        alert.show();
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
}
