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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.bytabit.app.ApplicationComponent;
import com.bytabit.app.R;
import com.bytabit.app.core.offer.manager.OfferManager;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.payment.model.CurrencyCode;
import com.bytabit.app.core.payment.model.PaymentMethod;
import com.bytabit.app.ui.BytabitApplication;
import com.bytabit.app.ui.MainActivity;
import com.bytabit.app.ui.common.CurrencyEditText;

import java.math.BigDecimal;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OfferAddFragment extends Fragment {

    private View view;

    private CompositeDisposable compositeDisposable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        compositeDisposable = new CompositeDisposable();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_offer_add, container, false);

        Single<OfferManager> offerManager = ((BytabitApplication) view.getContext()
                .getApplicationContext()).getApplicationComponent()
                .map(ApplicationComponent::offerManager);

        // add offer

        Observable<View> addButtonObservable = Observable.create(source ->
                view.findViewById(R.id.offer_add_button).setOnClickListener(source::onNext));

        compositeDisposable.add(addButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .map(v -> getOfferParams())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapSingle(o -> offerManager.map(om -> om.createOffer(o.getOfferType(),
                        o.getCurrencyCode(), o.getPaymentMethod(), o.getMinAmount(),
                        o.getMaxAmount(), o.getPrice())))
                .flatMapSingle(o -> o)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(o -> {
                    Snackbar.make(view, String.format("Added Offer id: %s", o.getId()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));

        return view;
    }

    private OfferParams getOfferParams() {

        Offer.OfferType offerType = (Offer.OfferType) ((Spinner) view.findViewById(R.id.offer_type_spinner)).getSelectedItem();
        CurrencyCode currencyCode = (CurrencyCode) ((Spinner) view.findViewById(R.id.offer_currency_spinner)).getSelectedItem();
        PaymentMethod paymentMethod = (PaymentMethod) ((Spinner) view.findViewById(R.id.offer_payment_method_spinner)).getSelectedItem();
        BigDecimal minAmount = ((CurrencyEditText) view.findViewById(R.id.offer_min_trade_amt_editor)).getBigDecimalAmount();
        BigDecimal maxAmount = ((CurrencyEditText) view.findViewById(R.id.offer_max_trade_amt_editor)).getBigDecimalAmount();
        BigDecimal price = ((CurrencyEditText) view.findViewById(R.id.offer_price_per_btc_editor)).getBigDecimalAmount();

        return new OfferParams(offerType, currencyCode, paymentMethod, minAmount, maxAmount, price);
    }

    private void showError(Throwable t) {
        log.error("Unable to add offer", t);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity mainActivity = ((MainActivity) getActivity());

        Spinner offerTypeSpinner = view.findViewById(R.id.offer_type_spinner);
        Spinner currencySpinner = view.findViewById(R.id.offer_currency_spinner);
        Spinner paymentMethodSpinner = view.findViewById(R.id.offer_payment_method_spinner);

        CurrencyEditText minAmountEditor = view.findViewById(R.id.offer_min_trade_amt_editor);
        CurrencyEditText maxAmountEditor = view.findViewById(R.id.offer_max_trade_amt_editor);
        CurrencyEditText priceAmountEditor = view.findViewById(R.id.offer_price_per_btc_editor);

        // populate offer type spinner
        Offer.OfferType[] offerTypes = Offer.OfferType.values();
        ArrayAdapter<Offer.OfferType> offerTypeAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_item, offerTypes);
        offerTypeSpinner.setAdapter(offerTypeAdapter);
        offerTypeSpinner.setSelection(0);

        // populate currency code spinner
        CurrencyCode[] currencyCodes = CurrencyCode.values();
        ArrayAdapter<CurrencyCode> currencyCodeAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_item, currencyCodes);
        currencySpinner.setAdapter(currencyCodeAdapter);
        currencySpinner.setSelection(0);

        // populate payment method spinner
        List<PaymentMethod> paymentMethods = currencyCodes[0].paymentMethods();
        ArrayAdapter<PaymentMethod> paymentMethodAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_item, paymentMethods);
        paymentMethodSpinner.setAdapter(paymentMethodAdapter);
        paymentMethodSpinner.setSelection(0);

        // clear price
        priceAmountEditor.setText("");

        // set currency spinner listener to populate payment methods when currency changed
        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {

                    CurrencyCode currencyCode = ((CurrencyCode) parent.getItemAtPosition(position));
                    // populate payment method spinner
                    List<PaymentMethod> paymentMethods = currencyCode.paymentMethods();
                    ArrayAdapter<PaymentMethod> paymentMethodAdapter = new ArrayAdapter<PaymentMethod>(view.getContext(), android.R.layout.simple_spinner_item, paymentMethods);
                    paymentMethodSpinner.setAdapter(paymentMethodAdapter);
                    paymentMethodSpinner.setSelection(0);

                    // set currency editors currency code
                    minAmountEditor.setCurrencyCode(currencyCode);
                    maxAmountEditor.setCurrencyCode(currencyCode);
                    priceAmountEditor.setCurrencyCode(currencyCode);

                    // set currency editor default amounts
                    minAmountEditor.setText(String.format("%s %s", currencyCode.getMinTradeAmount().toPlainString(), currencyCode));
                    maxAmountEditor.setText(String.format("%s %s", currencyCode.getMaxTradeAmount().toPlainString(), currencyCode));
                    priceAmountEditor.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // DO NOTHING
            }
        });

        // set title bar
        mainActivity.setActionBarTitle(R.string.offer_add_header);

        // hide floating action button
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

    @Value
    private class OfferParams {

        @lombok.NonNull
        private com.bytabit.app.core.offer.model.Offer.OfferType offerType;

        @lombok.NonNull
        private CurrencyCode currencyCode;

        @lombok.NonNull
        private PaymentMethod paymentMethod;

        @lombok.NonNull
        private BigDecimal minAmount;

        @lombok.NonNull
        private BigDecimal maxAmount;

        @lombok.NonNull
        private BigDecimal price;
    }
}
