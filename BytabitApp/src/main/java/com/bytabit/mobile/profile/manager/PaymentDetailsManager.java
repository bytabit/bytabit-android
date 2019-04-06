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

package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.file.StorageManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class PaymentDetailsManager {

    private static final String PROFILE_PAYMENT_DETAILS = "profile.paymentDetails";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PublishSubject<PaymentDetails> updatedPaymentDetails = PublishSubject.create();

    private final PublishSubject<PaymentDetails> removedPaymentDetails = PublishSubject.create();

    private final PublishSubject<PaymentDetails> selectedPaymentDetails = PublishSubject.create();

    @Inject
    StorageManager storageManager;

    public Observable<PaymentDetails> updatePaymentDetails(PaymentDetails paymentDetails) {
        return Observable.just(paymentDetails)
                .doOnNext(pd -> storageManager.store(paymentDetailsKey(paymentDetails.getCurrencyCode(),
                        paymentDetails.getPaymentMethod()), paymentDetails.getDetails()))
                .doOnNext(updatedPaymentDetails::onNext);
    }

    public Observable<PaymentDetails> removePaymentDetails(PaymentDetails paymentDetails) {
        return Observable.just(paymentDetails)
                .doOnNext(pd -> storageManager.remove(paymentDetailsKey(paymentDetails.getCurrencyCode(),
                        paymentDetails.getPaymentMethod())))
                .doOnNext(removedPaymentDetails::onNext);
    }

    public void setSelectedPaymentDetails(PaymentDetails paymentDetails) {
        selectedPaymentDetails.onNext(paymentDetails);
    }

    public Observable<PaymentDetails> getLoadedPaymentDetails() {

        return Observable.fromArray(CurrencyCode.values())
                .flatMap(c -> Observable.fromIterable(c.paymentMethods())
                        .map(p -> storageManager.retrieve(paymentDetailsKey(c, p)).map(pd ->
                                PaymentDetails.builder()
                                        .currencyCode(c)
                                        .paymentMethod(p)
                                        .details(pd).build()))
                        .filter(Optional::isPresent)
                        .map(Optional::get))
                .doOnNext(paymentDetails -> log.debug("Loaded: {}", paymentDetails))
                .cache();
    }

    public Observable<PaymentDetails> getUpdatedPaymentDetails() {
        return updatedPaymentDetails
                .doOnNext(paymentDetails -> log.debug("Updated: {}", paymentDetails))
                .share();
    }

    public Observable<PaymentDetails> getRemovedPaymentDetails() {
        return removedPaymentDetails
                .doOnNext(paymentDetails -> log.debug("Removed: {}", paymentDetails))
                .share();
    }

    public Observable<PaymentDetails> getSelectedPaymentDetails() {
        return selectedPaymentDetails
                .doOnNext(paymentDetails -> log.debug("Selected: {}", paymentDetails))
                .share();
    }

    private String paymentDetailsKey(CurrencyCode currencyCode,
                                     PaymentMethod paymentMethod) {

        return String.format("%s.%s.%s", PROFILE_PAYMENT_DETAILS, currencyCode.name(),
                paymentMethod.displayName());
    }
}