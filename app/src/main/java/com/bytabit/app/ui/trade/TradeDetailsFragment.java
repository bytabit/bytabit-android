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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;

import com.bytabit.app.ApplicationComponent;
import com.bytabit.app.R;
import com.bytabit.app.core.trade.manager.TradeManager;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.ui.BytabitApplication;
import com.bytabit.app.ui.MainActivity;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.bytabit.app.core.trade.model.Trade.Role.BUYER;
import static com.bytabit.app.core.trade.model.Trade.Role.SELLER;
import static com.bytabit.app.core.trade.model.Trade.Status.CANCELED;
import static com.bytabit.app.core.trade.model.Trade.Status.COMPLETED;

@Slf4j
public class TradeDetailsFragment extends Fragment {

    private Button fundEscrowButton;
    private Button paymentSentButton;
    private Button paymentReceivedButton;
    private Button cancelButton;
    private Button arbitrateButton;
    private Button refundSellerButton;
    private Button payoutBuyerButton;

    private List<Button> allButtons;

    private TextView statusText;
    private TextView roleText;
    private TextView paymentMethodText;
    private TextView paymentAmtText;
    private TextView paymentDetailsText;
    private TextView purchasedAmtText;
    private TextView txFeeText;
    private TextView payoutReasonText;
    private TextView arbitrateReasonText;

    private EditText paymentRefEdit;

    private TableRow paymentDetailsRow;
    private TableRow paymentRefRow;
    private TableRow txFeeRow;
    private TableRow payoutReasonRow;
    private TableRow arbitrateReasonRow;

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

        View view = inflater.inflate(R.layout.fragment_trade_details, container, false);

        // buttons
        fundEscrowButton = view.findViewById(R.id.trade_fund_escrow_button);
        paymentSentButton = view.findViewById(R.id.trade_payment_sent_button);
        paymentReceivedButton = view.findViewById(R.id.trade_payment_received_button);
        cancelButton = view.findViewById(R.id.trade_cancel_button);
        arbitrateButton = view.findViewById(R.id.trade_arbitrate_button);
        refundSellerButton = view.findViewById(R.id.trade_refund_seller_button);
        payoutBuyerButton = view.findViewById(R.id.trade_payout_buyer_button);

        allButtons = Arrays.asList(fundEscrowButton, paymentSentButton, paymentReceivedButton,
                cancelButton, arbitrateButton, refundSellerButton, payoutBuyerButton);

        // text views
        statusText = view.findViewById(R.id.trade_status_text);
        roleText = view.findViewById(R.id.trade_role_text);
        paymentMethodText = view.findViewById(R.id.trade_payment_method_text);
        paymentAmtText = view.findViewById(R.id.trade_payment_amt_text);
        purchasedAmtText = view.findViewById(R.id.trade_purchased_amt_text);
        paymentDetailsText = view.findViewById(R.id.trade_payment_details_text);
        txFeeText = view.findViewById(R.id.trade_tx_fee_text);
        payoutReasonText = view.findViewById(R.id.trade_payout_reason_text);
        arbitrateReasonText = view.findViewById(R.id.trade_arbitrate_reason_text);

        // edit field
        paymentRefEdit = view.findViewById(R.id.trade_payment_ref_edit);

        // rows
        paymentDetailsRow = view.findViewById(R.id.trade_payment_details_row);
        paymentRefRow = view.findViewById(R.id.trade_payment_ref_row);
        txFeeRow = view.findViewById(R.id.trade_tx_fee_row);
        payoutReasonRow = view.findViewById(R.id.trade_payout_reason_row);
        arbitrateReasonRow = view.findViewById(R.id.trade_arbitrate_reason_row);

        Single<TradeManager> tradeManager = ((BytabitApplication) view.getContext()
                .getApplicationContext()).getApplicationComponent()
                .map(ApplicationComponent::tradeManager);

        // show trade details

        compositeDisposable.add(tradeManager.flatMapObservable(TradeManager::getSelectedTrade)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::displayTrade));

        // fund escrow

        Observable<View> fundEscrowButtonObservable = Observable.create(source -> {
            fundEscrowButton.setOnClickListener(source::onNext);
        });

        compositeDisposable.add(fundEscrowButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(v -> disableEditorAndButtons())
                .observeOn(Schedulers.io())
                .flatMapSingle(v -> tradeManager.flatMap(TradeManager::fundEscrow))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(trade -> {
                    Snackbar.make(getView(), String.format("Funded trade id: %s", trade.getId()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));

        // payment sent

        Observable<View> paymentSentButtonObservable = Observable.create(source -> {
            paymentSentButton.setOnClickListener(source::onNext);
        });

        compositeDisposable.add(paymentSentButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(v -> disableEditorAndButtons())
                .map(v -> paymentRefEdit.getText().toString())
                .observeOn(Schedulers.io())
                .flatMapSingle(ref -> tradeManager.flatMap(tm -> tm.buyerSendPayment(ref)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(trade -> {
                    Snackbar.make(getView(), String.format("Payment sent with Ref: %s", trade.getPaymentReference()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));

        // payment received

        Observable<View> paymentReceivedButtonObservable = Observable.create(source -> {
            paymentReceivedButton.setOnClickListener(source::onNext);
        });

        compositeDisposable.add(paymentReceivedButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(v -> disableEditorAndButtons())
                .observeOn(Schedulers.io())
                .flatMapSingle(v -> tradeManager.flatMap(TradeManager::sellerPaymentReceived))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(trade -> {
                    Snackbar.make(getView(), String.format("Payment received with Ref: %s", trade.getPaymentReference()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));

        // cancel

        Observable<View> cancelButtonObservable = Observable.create(source -> {
            cancelButton.setOnClickListener(source::onNext);
        });

        compositeDisposable.add(cancelButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(v -> disableEditorAndButtons())
                .observeOn(Schedulers.io())
                .flatMapMaybe(v -> tradeManager.flatMapMaybe(TradeManager::cancelTrade))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(trade -> {
                    Snackbar.make(getView(), String.format("Canceled id: %s", trade.getId()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));

        // arbitrate

        Observable<View> arbitrateButtonObservable = Observable.create(source -> {
            arbitrateButton.setOnClickListener(source::onNext);
        });

        compositeDisposable.add(arbitrateButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(v -> disableEditorAndButtons())
                .observeOn(Schedulers.io())
                .flatMapSingle(v -> tradeManager.flatMap(TradeManager::requestArbitrate))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(trade -> {
                    Snackbar.make(getView(), String.format("Request arbitration for id: %s", trade.getId()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));


        // refund seller

        Observable<View> refundSellerButtonObservable = Observable.create(source -> {
            refundSellerButton.setOnClickListener(source::onNext);
        });

        compositeDisposable.add(refundSellerButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(v -> disableEditorAndButtons())
                .observeOn(Schedulers.io())
                .flatMapMaybe(v -> tradeManager.flatMapMaybe(TradeManager::arbitratorRefundSeller))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(trade -> {
                    Snackbar.make(getView(), String.format("Refund seller for id: %s", trade.getId()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));

        // payout buyer

        Observable<View> payoutBuyerButtonObservable = Observable.create(source -> {
            payoutBuyerButton.setOnClickListener(source::onNext);
        });

        compositeDisposable.add(payoutBuyerButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(v -> disableEditorAndButtons())
                .observeOn(Schedulers.io())
                .flatMapMaybe(v -> tradeManager.flatMapMaybe(TradeManager::arbitratorPayoutBuyer))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(trade -> {
                    Snackbar.make(getView(), String.format("Payout buyer for id: %s", trade.getId()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));


        return view;
    }

    private void displayTrade(Trade trade) {

        enableEditorAndButtons();

        showTradeDetails(trade);

        hideButtonsAndDisableRefField();

        switch (trade.getRole()) {

            case BUYER:
                showBuyerButtons(trade.getStatus());
                break;
            case SELLER:
                showSellerButtons(trade.getStatus());
                break;
            case ARBITRATOR:
                showArbitratorButtons(trade);
                break;
            default:
                // TODO throw error
                break;
        }
    }

    private void disableEditorAndButtons() {
        paymentRefEdit.setEnabled(false);

        for (Button button : allButtons) {
            button.setEnabled(false);
        }
    }

    private void enableEditorAndButtons() {
        paymentRefEdit.setEnabled(true);

        for (Button button : allButtons) {
            button.setEnabled(true);
        }
    }

    private void showTradeDetails(Trade trade) {

        String currencyCode = trade.getCurrencyCode().toString();
        int currencyScale = trade.getCurrencyCode().getScale();

        statusText.setText(trade.getStatus().toString());
        roleText.setText(trade.getRole().toString());
        paymentMethodText.setText(trade.getPaymentMethod().toString());
        paymentAmtText.setText(String.format(Locale.US, "%s %s", trade.getPaymentAmount().setScale(currencyScale, RoundingMode.HALF_UP).toPlainString(), currencyCode));
        purchasedAmtText.setText(String.format(Locale.US, "%s BTC", trade.getBtcAmount().setScale(8, RoundingMode.HALF_UP).toPlainString()));

        if (trade.hasPaymentRequest()) {
            paymentDetailsRow.setVisibility(VISIBLE);
            txFeeRow.setVisibility(VISIBLE);
            paymentDetailsText.setText(trade.getPaymentDetails());
            txFeeText.setText(String.format(Locale.US, "%s BTC/KB", trade.getTxFeePerKb().setScale(8, RoundingMode.HALF_UP).toPlainString()));
        } else {
            paymentDetailsRow.setVisibility(GONE);
            txFeeRow.setVisibility(GONE);
        }

        if ((BUYER.equals(trade.getRole()) && CANCELED.compareTo(trade.getStatus()) != 0 && trade.hasPaymentRequest())
                || (SELLER.equals(trade.getRole()) && trade.hasPayoutRequest())) {
            paymentRefRow.setVisibility(VISIBLE);
            paymentRefEdit.setText(trade.getPaymentReference());
        } else {
            paymentRefRow.setVisibility(GONE);
        }

        if (trade.hasPayoutCompleted()) {
            payoutReasonRow.setVisibility(VISIBLE);
            payoutReasonText.setText(trade.getPayoutReason().toString());
        } else {
            payoutReasonRow.setVisibility(GONE);
        }

        if (trade.hasArbitrateRequest()) {
            arbitrateReasonRow.setVisibility(VISIBLE);
            arbitrateReasonText.setText(trade.getArbitrationReason().toString());
        } else {
            arbitrateReasonRow.setVisibility(GONE);
        }
    }

    private void hideButtonsAndDisableRefField() {

        for (Button button : allButtons) {
            button.setVisibility(GONE);
        }

        paymentRefEdit.setEnabled(false);
    }

    private void showBuyerButtons(Trade.Status status) {

        switch (status) {

            case CREATED:
            case ACCEPTED:
            case FUNDING:
                cancelButton.setVisibility(VISIBLE);
                break;
            case FUNDED:
                paymentRefEdit.setEnabled(true);
                cancelButton.setVisibility(VISIBLE);
                paymentSentButton.setVisibility(VISIBLE);
                break;
            case PAID:
            case COMPLETING:
                arbitrateButton.setVisibility(VISIBLE);
                break;
            default:
                break;
        }
    }

    private void showSellerButtons(Trade.Status status) {

        switch (status) {

            case CREATED:
                cancelButton.setVisibility(VISIBLE);
                break;
            case ACCEPTED:
                cancelButton.setVisibility(VISIBLE);
                fundEscrowButton.setVisibility(VISIBLE);
                break;
            case FUNDING:
            case FUNDED:
                arbitrateButton.setVisibility(VISIBLE);
                break;
            case PAID:
                arbitrateButton.setVisibility(VISIBLE);
                paymentReceivedButton.setVisibility(VISIBLE);
                break;
            case COMPLETING:
                arbitrateButton.setVisibility(VISIBLE);
                break;
            default:
                break;
        }
    }

    private void showArbitratorButtons(Trade trade) {

        if (!COMPLETED.equals(trade.getStatus())) {

            refundSellerButton.setVisibility(VISIBLE);
            if (trade.hasPayoutRequest()) {
                payoutBuyerButton.setVisibility(VISIBLE);
            }
        }
    }

    private void showError(Throwable t) {
        log.error("trade details error", t);
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
        mainActivity.setActionBarTitle(R.string.trade_details_header);

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
}
