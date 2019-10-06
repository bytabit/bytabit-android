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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

import com.bytabit.app.ApplicationComponent;
import com.bytabit.app.R;
import com.bytabit.app.core.offer.OfferManager;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.ui.BytabitApplication;
import com.bytabit.app.ui.MainActivity;
import com.bytabit.app.ui.common.CurrencyEditText;
import com.bytabit.app.ui.trade.TradeListFragment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.bytabit.app.core.offer.model.Offer.OfferType.BUY;
import static com.bytabit.app.core.offer.model.Offer.OfferType.SELL;

@Slf4j
public class OfferDetailsFragment extends Fragment {

    private TextView typeText;
    private TextView currencyText;
    private TextView paymentMethodText;
    private TextView minTradeAmtText;
    private TextView maxTradeAmtText;
    private TextView pricePerBtcText;

    private CurrencyEditText currencyAmtEdit;
    private TextView btcAmtText;

    private Button removeButton;
    private Button takeButton;

    private TableRow currencyAmtRow;
    private TableRow btcAmtRow;

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

        View view = inflater.inflate(R.layout.fragment_offer_details, container, false);

        typeText = view.findViewById(R.id.offer_type_text);
        currencyText = view.findViewById(R.id.offer_currency_text);
        paymentMethodText = view.findViewById(R.id.offer_payment_method_text);
        minTradeAmtText = view.findViewById(R.id.offer_min_trade_amt_text);
        maxTradeAmtText = view.findViewById(R.id.offer_max_trade_amt_text);
        pricePerBtcText = view.findViewById(R.id.offer_price_per_btc_text);

        currencyAmtEdit = view.findViewById(R.id.offer_currency_amt_edit);
        btcAmtText = view.findViewById(R.id.offer_btc_amt_text);

        removeButton = view.findViewById(R.id.offer_remove_button);
        takeButton = view.findViewById(R.id.offer_take_button);

        currencyAmtRow = view.findViewById(R.id.offer_currency_amt_table_row);
        btcAmtRow = view.findViewById(R.id.offer_btc_amt_table_row);

        Single<OfferManager> offerManager = ((BytabitApplication) view.getContext()
                .getApplicationContext()).getApplicationComponent()
                .map(ApplicationComponent::offerManager);

        // update price based on currency amount and display offer

        compositeDisposable.add(offerManager.flatMapObservable(OfferManager::getSelectedOffer)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> {
                    currencyAmtEdit.setText("");
                    handleCurrencyAmtChange(currencyAmtEdit, o);
                    displayOffer(o);
                }));

        // delete offer

        Observable<View> deleteButtonObservable = Observable.create(source -> {
            removeButton.setOnClickListener(source::onNext);
        });

        compositeDisposable.add(deleteButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(v -> disableEditorAndButtons())
                .observeOn(Schedulers.io())
                .flatMapSingle(v -> offerManager.flatMap(OfferManager::deleteOffer))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(id -> {
                    Snackbar.make(getView(), String.format("Delete Offer id: %s", id), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));

        // take offer

        Observable<View> takeButtonObservable = Observable.create(source -> {
            takeButton.setOnClickListener(source::onNext);
        });

        compositeDisposable.add(takeButtonObservable.map(v -> getBtcAmount())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(v -> disableEditorAndButtons())
                .flatMapMaybe(ba -> offerManager.flatMapMaybe(om -> om.createTrade(ba)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(t -> {
                    Snackbar.make(getView(), String.format("Take Offer and create trade id: %s", t.getId()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    hideKeyboard();

                    // TODO switching fragments should be done via MainActivity so side menu item selection is also updated
                    TradeListFragment tradesFragment = ((MainActivity) getActivity()).getTradeListFragment();
                    getFragmentManager().beginTransaction()
                            .addToBackStack("main")
                            .replace(R.id.content_main, tradesFragment)
                            .commit();
                }));

        return view;
    }

    private BigDecimal getBtcAmount() {
        String REPLACEABLE = "[[a-z][A-Z],\\s]";
        return new BigDecimal(btcAmtText.getText().toString().replaceAll(REPLACEABLE, ""));
    }

    private void displayOffer(Offer offer) {

        enableEditorsAndButtons();

        String currencyCode = offer.getCurrencyCode().toString();
        int currencyScale = offer.getCurrencyCode().getScale();

        String offerType = getOfferType(offer).toString();

        typeText.setText(offerType);
        takeButton.setText(offerType);

        currencyText.setText(offer.getCurrencyCode().toString());
        paymentMethodText.setText(offer.getPaymentMethod().toString());
        minTradeAmtText.setText(String.format(Locale.US, "%s %s", offer.getMinAmount().setScale(currencyScale, RoundingMode.HALF_UP).toPlainString(), currencyCode));
        maxTradeAmtText.setText(String.format(Locale.US, "%s %s", offer.getMaxAmount().setScale(currencyScale, RoundingMode.HALF_UP).toPlainString(), currencyCode));
        pricePerBtcText.setText(String.format(Locale.US, "%s %s", offer.getPrice().setScale(currencyScale, RoundingMode.HALF_UP).toPlainString(), currencyCode));

        currencyAmtEdit.setCurrencyCode(offer.getCurrencyCode());

        if (offer.getIsMine()) {
            // my offer
            currencyAmtRow.setVisibility(GONE);
            btcAmtRow.setVisibility(GONE);
            takeButton.setVisibility(GONE);
            removeButton.setVisibility(VISIBLE);
        } else {
            // not my offer
            currencyAmtRow.setVisibility(VISIBLE);
            btcAmtRow.setVisibility(VISIBLE);
            takeButton.setVisibility(VISIBLE);
            removeButton.setVisibility(View.GONE);
        }
    }

    private void disableEditorAndButtons() {
        currencyAmtEdit.setEnabled(false);
        takeButton.setEnabled(false);
        removeButton.setEnabled(false);
    }

    private void enableEditorsAndButtons() {
        currencyAmtEdit.setEnabled(true);
        takeButton.setEnabled(true);
        removeButton.setEnabled(true);
    }

    private Offer.OfferType getOfferType(Offer offer) {
        if (offer.getIsMine()) {
            return offer.getOfferType();
        } else {
            // swap offer type if not my offer
            return SELL.equals(offer.getOfferType()) ? BUY : SELL;
        }
    }

    private void handleCurrencyAmtChange(final CurrencyEditText editText, Offer offer) {

        editText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (!editable.toString().equals(current)) {

                    BigDecimal currencyAmt = editText.getBigDecimalAmount();
                    BigDecimal priceAmt = offer.getPrice();

                    BigDecimal btcAmount = currencyAmt.divide(priceAmt, MathContext.DECIMAL64).setScale(8, BigDecimal.ROUND_HALF_UP);
                    btcAmtText.setText(btcAmount.toPlainString().concat(" BTC"));
                }
            }
        });
    }

    private void showError(Throwable t) {
        log.error("offer details error", t);
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

        // set title bar
        mainActivity.setActionBarTitle(R.string.offer_details_header);

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

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(MainActivity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }
}
