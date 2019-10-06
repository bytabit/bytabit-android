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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.bytabit.app.ApplicationComponent;
import com.bytabit.app.R;
import com.bytabit.app.core.wallet.WalletException;
import com.bytabit.app.core.wallet.WalletManager;
import com.bytabit.app.ui.BytabitApplication;
import com.bytabit.app.ui.MainActivity;

import org.bitcoinj.crypto.MnemonicCode;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletRestoreFragment extends Fragment {

    private AutoCompleteTextView word1Auto;
    private AutoCompleteTextView word2Auto;
    private AutoCompleteTextView word3Auto;
    private AutoCompleteTextView word4Auto;
    private AutoCompleteTextView word5Auto;
    private AutoCompleteTextView word6Auto;
    private AutoCompleteTextView word7Auto;
    private AutoCompleteTextView word8Auto;
    private AutoCompleteTextView word9Auto;
    private AutoCompleteTextView word10Auto;
    private AutoCompleteTextView word11Auto;
    private AutoCompleteTextView word12Auto;

    List<AutoCompleteTextView> wordAutos;

    private EditText restoreDateEdit;
    private Button restoreButton;

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

        View view = inflater.inflate(R.layout.fragment_wallet_restore, container, false);

        // autofills
        word1Auto = view.findViewById(R.id.wallet_restore_word1_auto);
        word2Auto = view.findViewById(R.id.wallet_restore_word2_auto);
        word3Auto = view.findViewById(R.id.wallet_restore_word3_auto);
        word4Auto = view.findViewById(R.id.wallet_restore_word4_auto);
        word5Auto = view.findViewById(R.id.wallet_restore_word5_auto);
        word6Auto = view.findViewById(R.id.wallet_restore_word6_auto);
        word7Auto = view.findViewById(R.id.wallet_restore_word7_auto);
        word8Auto = view.findViewById(R.id.wallet_restore_word8_auto);
        word9Auto = view.findViewById(R.id.wallet_restore_word9_auto);
        word10Auto = view.findViewById(R.id.wallet_restore_word10_auto);
        word11Auto = view.findViewById(R.id.wallet_restore_word11_auto);
        word12Auto = view.findViewById(R.id.wallet_restore_word12_auto);

        MnemonicCode mnemonicCode = MnemonicCode.INSTANCE;
        List<String> wordList = mnemonicCode.getWordList();

        wordAutos = Arrays.asList(
                word1Auto, word2Auto, word3Auto,
                word4Auto, word5Auto, word6Auto,
                word7Auto, word8Auto, word9Auto,
                word10Auto, word11Auto, word12Auto);

        for (AutoCompleteTextView wordAuto : wordAutos) {
            ArrayAdapter<String> seedWordAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_item, wordList);
            wordAuto.setAdapter(seedWordAdapter);
        }

        // edit field
        restoreDateEdit = view.findViewById(R.id.wallet_restore_first_use_date);

        // buttons
        restoreButton = view.findViewById(R.id.wallet_restore_button);


        Single<WalletManager> walletManager = ((BytabitApplication) view.getContext()
                .getApplicationContext()).getApplicationComponent()
                .map(ApplicationComponent::walletManager);

        // restore wallet

        Observable<View> restoreButtonObservable = Observable.create(source -> {
            restoreButton.setOnClickListener(source::onNext);
        });

        compositeDisposable.add(restoreButtonObservable
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(v -> disableEditorsAndButton())
                .map(v -> getRestoreWordsAndDate())
                .observeOn(Schedulers.io())
                .flatMapSingle(r -> walletManager.map(wm -> wm.restoreTradeWallet(r.getWords(), r.getRestoreDate())))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::showError).retry()
                .subscribe(walletKitConfig -> {
                    Snackbar.make(getView(), "Restored trade wallet", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    getFragmentManager().popBackStack();
                }));

        return view;
    }

    private void disableEditorsAndButton() {
        for (AutoCompleteTextView wordAuto : wordAutos) {
            wordAuto.setEnabled(false);
        }
        restoreDateEdit.setEnabled(false);
        restoreButton.setEnabled(false);
    }

    private RestoreWordsAndDate getRestoreWordsAndDate() {

        List<String> words = new ArrayList<>();

        for (AutoCompleteTextView wordAuto : wordAutos) {
            words.add(wordAuto.getText().toString());
        }

        DateFormat dateFormat = new SimpleDateFormat(getContext().getString(R.string.wallet_restore_date_format));
        String restoreDateStr = restoreDateEdit.getText().toString();
        Date restoreDate;
        try {
            restoreDate = dateFormat.parse(restoreDateStr);
        } catch (ParseException pe) {
            log.error("Could not parse restore date, {}", restoreDateStr, pe);
            throw new WalletException(String.format("Could not parse restore date %s.", restoreDateStr), pe);
        }

        return new RestoreWordsAndDate(words, restoreDate);
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
        mainActivity.setActionBarTitle(R.string.wallet_restore_header);

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
    private class RestoreWordsAndDate {

        private List<String> words;
        private Date restoreDate;
    }
}
