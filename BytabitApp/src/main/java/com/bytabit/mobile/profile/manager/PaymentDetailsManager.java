package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.StorageManager;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Optional;

public class PaymentDetailsManager {

    private final String PROFILE_PAYMENT_DETAILS = "profile.paymentDetails";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PublishSubject<PaymentDetails> updatedPaymentDetails = PublishSubject.create();

    private final PublishSubject<PaymentDetails> removedPaymentDetails = PublishSubject.create();

    private final PublishSubject<PaymentDetails> selectedPaymentDetails = PublishSubject.create();

    @Inject
    StorageManager storageManager;

    @PostConstruct
    public void initialize() {

    }

    public void updatePaymentDetails(PaymentDetails paymentDetails) {
        Observable.fromCallable(() -> {
            storageManager.store(paymentDetailsKey(paymentDetails.getCurrencyCode(),
                    paymentDetails.getPaymentMethod()), paymentDetails.getPaymentDetails());
            return paymentDetails;
        }).subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .subscribe(updatedPaymentDetails::onNext);
    }

    public void removePaymentDetails(PaymentDetails paymentDetails) {
        Observable.fromCallable(() -> {
            storageManager.remove(paymentDetailsKey(paymentDetails.getCurrencyCode(),
                            paymentDetails.getPaymentMethod()));
                    return paymentDetails;
                }
        ).subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .subscribe(removedPaymentDetails::onNext);
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
                                        .paymentDetails(pd).build()))
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