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

package com.bytabit.app.ui;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bytabit.app.ApplicationComponent;
import com.bytabit.app.R;
import com.bytabit.app.core.badge.model.Badge;
import com.bytabit.app.core.net.TorManager;
import com.bytabit.app.core.offer.manager.OfferManager;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.payment.model.PaymentDetails;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.ui.badge.BadgeBuyFragment;
import com.bytabit.app.ui.badge.BadgeListFragment;
import com.bytabit.app.ui.offer.OfferAddFragment;
import com.bytabit.app.ui.offer.OfferDetailsFragment;
import com.bytabit.app.ui.offer.OfferListFragment;
import com.bytabit.app.ui.payment.PaymentDetailsFragment;
import com.bytabit.app.ui.payment.PaymentListFragment;
import com.bytabit.app.ui.trade.TradeDetailsFragment;
import com.bytabit.app.ui.trade.TradeListFragment;
import com.bytabit.app.ui.wallet.WalletDepositFragment;
import com.bytabit.app.ui.wallet.WalletFragment;
import com.bytabit.app.ui.wallet.WalletRestoreFragment;

import java.io.IOException;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.net.ConnectivityManager.EXTRA_NO_CONNECTIVITY;

@Slf4j
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        OfferListFragment.OnListFragmentInteractionListener, TradeListFragment.OnListFragmentInteractionListener,
        PaymentListFragment.OnListFragmentInteractionListener, BadgeListFragment.OnListFragmentInteractionListener {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // offer fragments

    private final OfferListFragment offerListFragment = new OfferListFragment();

    private final OfferDetailsFragment offerDetailsFragment = new OfferDetailsFragment();

    private final OfferAddFragment offerAddFragment = new OfferAddFragment();

    // trade fragments

    private final TradeListFragment tradeListFragment = new TradeListFragment();
    private final TradeDetailsFragment tradeDetailsFragment = new TradeDetailsFragment();

    // wallet fragments

    private final WalletFragment walletFragment = new WalletFragment();

    private final WalletDepositFragment walletDepositFragment = new WalletDepositFragment();

    private final WalletRestoreFragment walletRestoreFragment = new WalletRestoreFragment();

    // payment fragments

    private final PaymentListFragment paymentListFragment = new PaymentListFragment();

    private final PaymentDetailsFragment paymentDetailsFragment = new PaymentDetailsFragment();

    // badge fragments

    private final BadgeListFragment badgeListFragment = new BadgeListFragment();
    private final BadgeBuyFragment badgeBuyFragment = new BadgeBuyFragment();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // start tor

        compositeDisposable.add(((BytabitApplication) getApplication()).getApplicationComponent()
                .map(ApplicationComponent::torManager)
                .flatMapObservable(tm -> tm.startTor())
                .observeOn(Schedulers.io())
                .doOnDispose(() -> {
                    log.info("torManager.start disposed.");
                })
                .doOnComplete(() -> {
                    log.info("torManager.start complete.");
                    compositeDisposable.add(((BytabitApplication) getApplication()).getApplicationComponent()
                            .map(ApplicationComponent::torManager)
                            .subscribe(tm -> {
                                        NetworkStateReceiver networkStateReceiver = new MainActivity.NetworkStateReceiver(tm);
                                        IntentFilter filter = new IntentFilter(CONNECTIVITY_ACTION);
                                        getApplicationContext().registerReceiver(networkStateReceiver, filter);
                                    }
                            ));
                })
                .doOnError(this::showError)
                .subscribe(s -> {
                    log.info("Tor state: {}", s.toString());
                }));

        // setup tool bar

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // setup drawer

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // During initial setup, plug in the offers fragment

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_main, new OfferListFragment()).commit();
            setTitle(getString(R.string.menu_offer_list));
            navigationView.getMenu().getItem(0).setChecked(true);
        }

        // update my offers
        Disposable updateOffersDisposable = ((BytabitApplication) getApplicationContext())
                .getApplicationComponent().map(ApplicationComponent::offerManager)
                .flatMapObservable(OfferManager::getUpdatedOffers)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError)
                .retry()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(trade -> log.debug("updated my offer: {}", trade));

        compositeDisposable.add(updateOffersDisposable);
    }

    private void showError(Throwable t) {
        log.error("Unable to update offer", t);
        AlertDialog.Builder alert = new AlertDialog.Builder(getApplicationContext());
        alert.setTitle(t.getMessage());

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // do nothing
            }
        });

        alert.show();
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds offerList to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Fragment fragment = null;

        if (id == R.id.nav_offers) {
            Toast.makeText(MainActivity.this, getResources().getText(R.string.menu_offer_list), Toast.LENGTH_SHORT).show();
            fragment = offerListFragment;
        } else if (id == R.id.nav_trades) {
            Toast.makeText(MainActivity.this, getResources().getText(R.string.menu_trade_list), Toast.LENGTH_SHORT).show();
            fragment = tradeListFragment;
        } else if (id == R.id.nav_wallet) {
            Toast.makeText(MainActivity.this, getResources().getText(R.string.menu_wallet), Toast.LENGTH_SHORT).show();
            fragment = walletFragment;
        } else if (id == R.id.nav_payment) {
            Toast.makeText(MainActivity.this, getResources().getText(R.string.menu_payment_list), Toast.LENGTH_SHORT).show();
            fragment = paymentListFragment;
        } else if (id == R.id.nav_badges) {
            Toast.makeText(MainActivity.this, getResources().getText(R.string.menu_badge_list), Toast.LENGTH_SHORT).show();
            fragment = badgeListFragment;
        } else if (id == R.id.nav_help) {
            Toast.makeText(MainActivity.this, getResources().getText(R.string.menu_help), Toast.LENGTH_SHORT).show();
            try {
                Intent startTelegram = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=bytabit"));
                startActivity(startTelegram);
            } catch (ActivityNotFoundException anf) {
                // https://play.google.com/store/apps/details?id=org.telegram.messenger
                Intent installTelegram = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=org.telegram.messenger"));
                startActivity(installTelegram);
            }
        }

        if (fragment != null) {

            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .addToBackStack("main")
                    .replace(R.id.content_main, fragment)
                    .commit();

            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Log.e("MainActivity", "Error in creating fragment");
        }
        return true;
    }

    public void setActionBarTitle(int titleId) {
        getSupportActionBar().setTitle(titleId);
    }

    public void showAddFab(View.OnClickListener addListener) {
        FloatingActionButton fab = findViewById(R.id.add_fab);
        fab.setOnClickListener(addListener);
        fab.show();
    }

    public void hideAddFab() {
        ((FloatingActionButton) findViewById(R.id.add_fab)).hide();
    }

    public void showRemoveFab(View.OnClickListener removeListener) {
        FloatingActionButton fab = findViewById(R.id.remove_fab);
        fab.setOnClickListener(removeListener);
        fab.show();
    }

    public void hideRemoveFab() {
        ((FloatingActionButton) findViewById(R.id.remove_fab)).hide();
    }

    // TODO
    @Override
    public void onListFragmentInteraction(Offer offer) {
        Snackbar.make(findViewById(R.id.nav_view), String.format("offer id: %s, content: %s", offer.getId(), offer.getMakerProfilePubKey()), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

        FragmentManager fragmentManager = getSupportFragmentManager();

        compositeDisposable.add(((BytabitApplication) getApplication()).getApplicationComponent()
                .map(ApplicationComponent::offerManager)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(om -> om.setSelectedOffer(offer)));

        fragmentManager.beginTransaction().addToBackStack("main")
                .replace(R.id.content_main, offerDetailsFragment)
                .commit();
    }

    public OfferAddFragment getOfferAddFragment() {
        return offerAddFragment;
    }

    @Override
    public void onListFragmentInteraction(Trade trade) {

        FragmentManager fragmentManager = getSupportFragmentManager();

        compositeDisposable.add(((BytabitApplication) getApplication()).getApplicationComponent()
                .map(ApplicationComponent::tradeManager)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(tm -> tm.setSelectedTrade(trade)));

        fragmentManager.beginTransaction().addToBackStack("main")
                .replace(R.id.content_main, tradeDetailsFragment)
                .commit();
    }

    public WalletFragment getWalletFragment() {
        return walletFragment;
    }

    public WalletDepositFragment getWalletDepositFragment() {
        return walletDepositFragment;
    }

    public WalletRestoreFragment getWalletRestoreFragment() {
        return walletRestoreFragment;
    }

    @Override
    public void onListFragmentInteraction(PaymentDetails paymentDetails) {
        Snackbar.make(findViewById(R.id.nav_view), String.format("payment details id: %s, content: %s", paymentDetails.getId(), paymentDetails.getCurrencyCode()), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

        FragmentManager fragmentManager = getSupportFragmentManager();

        compositeDisposable.add(((BytabitApplication) getApplication()).getApplicationComponent()
                .map(ApplicationComponent::paymentDetailsManager)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(pdm -> pdm.setSelectedPaymentDetails(paymentDetails)));

        fragmentManager.beginTransaction().addToBackStack("main")
                .replace(R.id.content_main, paymentDetailsFragment)
                .commit();
    }

    public PaymentDetailsFragment getPaymentDetailsFragment() {
        return paymentDetailsFragment;
    }

    @Override
    public void onListFragmentInteraction(Badge badge) {
        Snackbar.make(findViewById(R.id.nav_view), String.format("badge id: %s, content: %s", badge.getId(), badge.getCurrencyCode()), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    public BadgeBuyFragment getBadgeBuyFragment() {
        return badgeBuyFragment;
    }

    public TradeListFragment getTradeListFragment() {
        return tradeListFragment;
    }

    private class NetworkStateReceiver extends BroadcastReceiver {

        private TorManager torManager;

        public NetworkStateReceiver(TorManager torManager) {
            this.torManager = torManager;
        }

        @Override
        public void onReceive(final Context ctx, final Intent i) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    boolean online = !i.getBooleanExtra(EXTRA_NO_CONNECTIVITY, false);
                    if (online) {
                        // Some devices fail to set EXTRA_NO_CONNECTIVITY, double check
                        Object o = ctx.getSystemService(CONNECTIVITY_SERVICE);
                        ConnectivityManager cm = (ConnectivityManager) o;
                        NetworkInfo net = cm.getActiveNetworkInfo();
                        if (net == null || !net.isConnected()) {
                            online = false;
                        }
                    }
                    log.info("Online: " + online);
                    try {
                        torManager.enableNetwork(online);
                    } catch (IOException e) {
                        log.warn(e.toString(), e);
                    }
                }
            }).start();
        }
    }
}
