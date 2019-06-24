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

package com.bytabit.app.core.payment.manager;

import com.bytabit.app.core.payment.model.PaymentDetails;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class PaymentDetailsManager {

    private final PublishSubject<PaymentDetails> updatedPaymentDetails = PublishSubject.create();

    private final PublishSubject<String> removedPaymentDetails = PublishSubject.create();

    private final BehaviorSubject<PaymentDetails> selectedPaymentDetails = BehaviorSubject.create();

    private final PaymentDetailsStorage paymentDetailsStorage;

    @Inject
    public PaymentDetailsManager(PaymentDetailsStorage paymentDetailsStorage) {
        this.paymentDetailsStorage = paymentDetailsStorage;
    }

    public Observable<PaymentDetails> updatePaymentDetails(PaymentDetails paymentDetails) {
        return Observable.just(paymentDetails)
                .flatMapSingle(pd -> paymentDetailsStorage.write(paymentDetails))
                .doOnNext(updatedPaymentDetails::onNext);
    }

    public Observable<String> removePaymentDetails(PaymentDetails paymentDetails) {
        return Observable.just(paymentDetails)
                .flatMapSingle(pd -> paymentDetailsStorage.delete(paymentDetails.getId()))
                .doOnNext(removedPaymentDetails::onNext);
    }

    public void setSelectedPaymentDetails(PaymentDetails paymentDetails) {
        selectedPaymentDetails.onNext(paymentDetails);
    }

    public Observable<PaymentDetails> getLoadedPaymentDetails() {

        return paymentDetailsStorage.getAll()
                .flattenAsObservable(l -> l)
                .doOnNext(paymentDetails -> log.debug("Loaded: {}", paymentDetails))
                .cache();
    }

    public Observable<PaymentDetails> getUpdatedPaymentDetails() {
        return updatedPaymentDetails
                .doOnNext(paymentDetails -> log.debug("Updated: {}", paymentDetails))
                .share();
    }

    public Observable<String> getRemovedPaymentDetails() {
        return removedPaymentDetails
                .doOnNext(paymentDetails -> log.debug("Removed: {}", paymentDetails))
                .share();
    }

    public Observable<PaymentDetails> getSelectedPaymentDetails() {
        return selectedPaymentDetails
                .doOnNext(paymentDetails -> log.debug("Selected: {}", paymentDetails))
                .share();
    }
}