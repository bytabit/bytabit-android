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

package com.bytabit.app.core.offer;

import com.bytabit.app.core.common.AppConfig;
import com.bytabit.app.core.net.RetrofitService;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.offer.model.SignedOffer;
import com.bytabit.app.core.wallet.WalletManager;

import org.bitcoinj.core.Sha256Hash;

import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class OfferService extends RetrofitService {

    private final OfferServiceApi offerServiceApi;
    private final WalletManager walletManager;

    @Inject
    public OfferService(AppConfig appConfig, WalletManager walletManager) {
        super(appConfig);
        this.walletManager = walletManager;
        // create an instance of the ApiService
        this.offerServiceApi = retrofit.create(OfferServiceApi.class);
    }

    Single<SignedOffer> put(Offer offer) {
        Single<SignedOffer> signedOffer = signOffer(offer);
        Single<SignedOffer> putSignedOffer = signedOffer.flatMap(so -> offerServiceApi.put(so.getId(), so));

        return putSignedOffer
                .doOnError(t -> log.error("put error: {}", t.getMessage()));
    }

    Single<List<SignedOffer>> getAll() {
        return offerServiceApi.get()
                .doOnError(t -> log.error("get error: {}", t.getMessage()))
                .flattenAsObservable(so -> so)
                .toList();
    }

    Single<SignedOffer> get(String id) {
        return offerServiceApi.get(id)
                .doOnError(t -> log.error("get error: {}", t.getMessage()))
                .filter(this::validateSignedOfferSignature).toSingle()
                .onErrorResumeNext(t -> {
                    if (t instanceof NoSuchElementException) {
                        return Single.error(new OfferException(String.format("No valid offer found for id %s.", id)));
                    } else {
                        return Single.error(t);
                    }
                });
    }

    Single<SignedOffer> delete(String id) {

        return offerServiceApi.delete(id)
                .doOnError(t -> log.error("delete error: {}", t.getMessage()));
    }

    private Single<SignedOffer> signOffer(Offer offer) {
        Sha256Hash hash = offer.sha256Hash();
        Single<String> signature = walletManager.getBase58PubKeySignature(hash);

        return signature.map(s -> createSignedOffer(offer, s));
    }

    private boolean validateSignedOfferSignature(SignedOffer signedOffer) {
        Sha256Hash hash = signedOffer.sha256Hash();
        return walletManager.validateBase58PubKeySignature(signedOffer.getMakerProfilePubKey(),
                signedOffer.getSignature(), hash);
    }

    private SignedOffer createSignedOffer(Offer o, String signature) {
        return SignedOffer.signedBuilder()
                .id(o.getId())
                .offerType(o.getOfferType())
                .makerProfilePubKey(o.getMakerProfilePubKey())
                .currencyCode(o.getCurrencyCode())
                .paymentMethod(o.getPaymentMethod())
                .minAmount(o.getMinAmount())
                .maxAmount(o.getMaxAmount())
                .price(o.getPrice())
                .signature(signature)
                .build();
    }
}
