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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.bytabit.app.ApplicationComponent;
import com.bytabit.app.R;
import com.bytabit.app.core.payment.PaymentDetailsManager;
import com.bytabit.app.core.payment.model.CurrencyCode;
import com.bytabit.app.core.payment.model.PaymentDetails;
import com.bytabit.app.core.payment.model.PaymentMethod;
import com.bytabit.app.ui.BytabitApplication;
import com.bytabit.app.ui.MainActivity;

import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentDetailsFragment extends Fragment {

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

        view = inflater.inflate(R.layout.fragment_payment_details, container, false);
        Spinner currencySpinner = view.findViewById(R.id.payment_currency_spinner);
        Spinner paymentMethodSpinner = view.findViewById(R.id.payment_method_spinner);
        EditText paymentDetailsEditor = view.findViewById(R.id.payment_details_edit);

        Single<PaymentDetailsManager> paymentDetailsManager = ((BytabitApplication) view.getContext()
                .getApplicationContext()).getApplicationComponent()
                .map(ApplicationComponent::paymentDetailsManager);

        // display selected payment details

        compositeDisposable.add(paymentDetailsManager.flatMapObservable(PaymentDetailsManager::getSelectedPaymentDetails)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::displayPaymentDetails));

        // add payment details button

        Observable<View> addButtonObservable = Observable.create(source -> {
            view.findViewById(R.id.payment_add_button).setOnClickListener(source::onNext);
        });

        compositeDisposable.add(addButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .map(v -> {
                    CurrencyCode currencyCode = (CurrencyCode) currencySpinner.getSelectedItem();
                    PaymentMethod paymentMethod = (PaymentMethod) paymentMethodSpinner.getSelectedItem();
                    String details = paymentDetailsEditor.getText().toString();
                    PaymentDetails pd = PaymentDetails.builder()
                            .currencyCode(currencyCode)
                            .paymentMethod(paymentMethod)
                            .details(details)
                            .build();
                    return new PaymentDetailsView(pd, v);
                })
                .observeOn(Schedulers.io())
                .flatMap(pdv -> paymentDetailsManager.flatMapObservable(pdm -> pdm.updatePaymentDetails(pdv.getPaymentDetails())
                        .map(pd -> new PaymentDetailsView(pd, pdv.getView()))
                ))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pdv -> {
                    hideKeyboard();
                    Snackbar.make(pdv.getView(), String.format("Added payment details for: %s %s",
                            pdv.getPaymentDetails().getCurrencyCode(),
                            pdv.getPaymentDetails().getPaymentMethod()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));

        // remove payment details button

        Observable<View> deleteButtonObservable = Observable.create(source -> {
            view.findViewById(R.id.payment_delete_button).setOnClickListener(source::onNext);
        });

        compositeDisposable.add(deleteButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .map(v -> {
                    CurrencyCode currencyCode = (CurrencyCode) currencySpinner.getSelectedItem();
                    PaymentMethod paymentMethod = (PaymentMethod) paymentMethodSpinner.getSelectedItem();
                    String details = paymentDetailsEditor.getText().toString();
                    PaymentDetails pd = PaymentDetails.builder()
                            .currencyCode(currencyCode)
                            .paymentMethod(paymentMethod)
                            .details(details)
                            .build();
                    return new PaymentDetailsView(pd, v);
                })
                .observeOn(Schedulers.io())
                .flatMap(pdv -> paymentDetailsManager.flatMapObservable(pdm -> pdm.removePaymentDetails(pdv.getPaymentDetails()))
                        .map(pd -> pdv)
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pdv -> {
                    hideKeyboard();
                    Snackbar.make(pdv.getView(), String.format("Removed payment details for: %s %s",
                            pdv.getPaymentDetails().getCurrencyCode(),
                            pdv.getPaymentDetails().getPaymentMethod()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));

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
        mainActivity.setActionBarTitle(R.string.payment_details_header);

        // hide floating action button
        mainActivity.hideAddFab();
        mainActivity.hideRemoveFab();

        clear();
    }

    public void clear() {
        displayPaymentDetails(PaymentDetails.builder()
                .currencyCode(CurrencyCode.SEK)
                .paymentMethod(CurrencyCode.SEK.paymentMethods().get(0))
                .details("")
                .build());
    }

    private void displayPaymentDetails(PaymentDetails paymentDetails) {

        Spinner currencySpinner = view.findViewById(R.id.payment_currency_spinner);
        Spinner paymentMethodSpinner = view.findViewById(R.id.payment_method_spinner);
        EditText paymentDetailsEditor = view.findViewById(R.id.payment_details_edit);

        // populate currency code spinner
        currencySpinner.setOnItemSelectedListener(null);
        CurrencyCode currencyCode = paymentDetails.getCurrencyCode();
        List<CurrencyCode> currencyCodes = Arrays.asList(CurrencyCode.values());
        ArrayAdapter<CurrencyCode> currencyCodeAdapter = new ArrayAdapter<CurrencyCode>(view.getContext(), android.R.layout.simple_spinner_item, currencyCodes);
        currencySpinner.setAdapter(currencyCodeAdapter);
        currencySpinner.setSelection(currencyCodeAdapter.getPosition(currencyCode));

        // populate payment method spinner and set listener for when currency code changed
        List<PaymentMethod> paymentMethods = currencyCode.paymentMethods();
        PaymentMethod paymentMethod = paymentDetails.getPaymentMethod();
        ArrayAdapter<PaymentMethod> paymentMethodAdapter = new ArrayAdapter<PaymentMethod>(view.getContext(), android.R.layout.simple_spinner_item, paymentMethods);
        paymentMethodSpinner.setAdapter(paymentMethodAdapter);
        paymentMethodSpinner.setSelection(paymentMethodAdapter.getPosition(paymentMethod));

        // populate payment details editor
        String details = paymentDetails.getDetails();
        paymentDetailsEditor.setText(details);

        // set currency spinner listener to populate payment methods when currency changed
        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    List<PaymentMethod> paymentMethods = ((CurrencyCode) parent.getItemAtPosition(position)).paymentMethods();
                    ArrayAdapter<PaymentMethod> paymentMethodAdapter = new ArrayAdapter<PaymentMethod>(view.getContext(), android.R.layout.simple_spinner_item, paymentMethods);
                    paymentMethodSpinner.setAdapter(paymentMethodAdapter);
                    paymentMethodSpinner.setSelection(paymentMethodAdapter.getPosition(paymentMethod));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // DO NOTHING
            }
        });
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

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(MainActivity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Value
    @AllArgsConstructor
    private class PaymentDetailsView {
        private PaymentDetails paymentDetails;
        private View view;
    }
}
