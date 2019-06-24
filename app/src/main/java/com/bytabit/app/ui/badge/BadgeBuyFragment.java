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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.bytabit.app.ApplicationComponent;
import com.bytabit.app.R;
import com.bytabit.app.core.badge.manager.BadgeException;
import com.bytabit.app.core.badge.manager.BadgeManager;
import com.bytabit.app.core.badge.model.Badge;
import com.bytabit.app.core.payment.model.CurrencyCode;
import com.bytabit.app.core.payment.model.PaymentMethod;
import com.bytabit.app.ui.BytabitApplication;
import com.bytabit.app.ui.MainActivity;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BadgeBuyFragment extends Fragment {

    Spinner badgeTypeSpinner;
    Spinner currencySpinner;

    TextView validFromLabel;
    TextView validToLabel;
    TextView priceAmtLabel;

    Button buyButton;

    private CompositeDisposable compositeDisposable;

    private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        compositeDisposable = new CompositeDisposable();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_badge_buy, container, false);

        buyButton = view.findViewById(R.id.badge_buy_button);
        badgeTypeSpinner = view.findViewById(R.id.badge_type_spinner);
        currencySpinner = view.findViewById(R.id.badge_currency_spinner);
        validFromLabel = view.findViewById(R.id.badge_validfrom_date_label);
        validToLabel = view.findViewById(R.id.badge_validto_date_label);
        priceAmtLabel = view.findViewById(R.id.badge_price_amt_label);

        Single<BadgeManager> badgeManager = ((BytabitApplication) view.getContext()
                .getApplicationContext()).getApplicationComponent()
                .map(ApplicationComponent::badgeManager);

        // buy badge button

        Observable<View> buyButtonObservable = Observable.create(source -> {
            buyButton.setOnClickListener(source::onNext);
        });

        compositeDisposable.add(buyButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(v -> disableSpinnersAndButton())
                .map(v -> getBadgeParams())
                .observeOn(Schedulers.io())
                .doOnNext(bp -> log.debug("Buy badge: {}", bp))
                .flatMap(bp -> badgeManager.flatMapObservable(bm -> bm.buyBadge(bp.getBadgeType(),
                        bp.getCurrencyCode(), bp.getAmount(),
                        bp.getValidFrom(), bp.getValidTo()).toObservable()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(b -> {
                    Snackbar.make(getView(), String.format("Bought badge for: %s %s",
                            b.getBadgeType(), b.getCurrencyCode()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));

        return view;
    }

    private void disableSpinnersAndButton() {
        badgeTypeSpinner.setEnabled(false);
        currencySpinner.setEnabled(false);
        buyButton.setEnabled(false);
    }

    private void enableSpinnersAndButton() {
        badgeTypeSpinner.setEnabled(true);
        currencySpinner.setEnabled(true);
        buyButton.setEnabled(true);
    }

    private BadgeParams getBadgeParams() {

        Badge.BadgeType badgeType = (Badge.BadgeType) badgeTypeSpinner.getSelectedItem();
        CurrencyCode currencyCode = (CurrencyCode) currencySpinner.getSelectedItem();

        try {
            Date validFrom = DATE_FORMAT.parse(validFromLabel.getText().toString());
            Date validTo = DATE_FORMAT.parse(validToLabel.getText().toString());

            return new BadgeParams(badgeType, validFrom, validTo, badgeType.price(), currencyCode,
                    null, null);

        } catch (ParseException pa) {
            throw new BadgeException("Couldn't parse date", pa);
        }
    }

    private void showError(Throwable t) {
        log.error("Unable to buy badge", t);
        AlertDialog.Builder alert = new AlertDialog.Builder(this.getContext());
        alert.setTitle(t.getMessage());

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .addToBackStack("main")
                        .replace(R.id.content_main, ((MainActivity) getContext()).getWalletFragment())
                        .commit();
            }
        });

        alert.show();
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
        mainActivity.setActionBarTitle(R.string.badges_buy_header);

        // hide floating action button
        mainActivity.hideAddFab();
        mainActivity.hideRemoveFab();

        clear();
    }

    public void clear() {

        Badge.BadgeType badgeType = Badge.BadgeType.OFFER_MAKER;
        Date validFrom = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(validFrom);
        calendar.add(Calendar.WEEK_OF_YEAR, badgeType.weeksValid());
        Date validTo = calendar.getTime();
        displayBadge(new BadgeParams(badgeType, validFrom, validTo, badgeType.price(),
                CurrencyCode.SEK, null, null));

        enableSpinnersAndButton();
    }

    private void displayBadge(BadgeParams badge) {

        // populate badge type spinner and set listener for when currency code changed
        Badge.BadgeType[] badgeTypes = {Badge.BadgeType.OFFER_MAKER};
        Badge.BadgeType badgeType = badge.getBadgeType();
        ArrayAdapter<Badge.BadgeType> badgeTypeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, badgeTypes);
        badgeTypeSpinner.setAdapter(badgeTypeAdapter);
        badgeTypeSpinner.setSelection(badgeTypeAdapter.getPosition(badgeType));

        // populate currency code spinner
        CurrencyCode currencyCode = badge.getCurrencyCode();
        CurrencyCode[] currencyCodes = CurrencyCode.values();
        ArrayAdapter<CurrencyCode> currencyCodeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, currencyCodes);
        currencySpinner.setAdapter(currencyCodeAdapter);
        currencySpinner.setSelection(currencyCodeAdapter.getPosition(currencyCode));

        // populate price amt and valid from to labels
        priceAmtLabel.setText(badge.getBadgeType().price().toPlainString());
        validFromLabel.setText(DATE_FORMAT.format(badge.getValidFrom()));
        validToLabel.setText(DATE_FORMAT.format(badge.getValidTo()));
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

    @Value
    private class BadgeParams {

        @lombok.NonNull
        private Badge.BadgeType badgeType;

        @lombok.NonNull
        private Date validFrom;

        @lombok.NonNull
        private Date validTo;

        @lombok.NonNull
        private BigDecimal amount;

        private CurrencyCode currencyCode;

        private PaymentMethod paymentMethod;

        private String detailsHash;
    }
}
