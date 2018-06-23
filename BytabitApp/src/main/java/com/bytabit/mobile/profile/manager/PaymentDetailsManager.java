package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentDetails;
import com.bytabit.mobile.profile.model.PaymentMethod;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import javax.annotation.PostConstruct;
import java.util.Optional;

public class PaymentDetailsManager extends AbstractManager {

    private final String PROFILE_PAYMENT_DETAILS = "profile.paymentDetails";

    private final EventLogger eventLogger = EventLogger.of(PaymentDetailsManager.class);

    private final PublishSubject<PaymentDetails> updatedPaymentDetails = PublishSubject.create();

    private final PublishSubject<PaymentDetails> removedPaymentDetails = PublishSubject.create();

    private final PublishSubject<PaymentDetails> selectedPaymentDetails = PublishSubject.create();

    @PostConstruct
    public void initialize() {

    }

    public void updatePaymentDetails(PaymentDetails paymentDetails) {
        Observable.fromCallable(() -> {
            store(paymentDetailsKey(paymentDetails.getCurrencyCode(),
                    paymentDetails.getPaymentMethod()), paymentDetails.getPaymentDetails());
            return paymentDetails;
        }).subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .subscribe(updatedPaymentDetails::onNext);
    }

    public void removePaymentDetails(PaymentDetails paymentDetails) {
        Observable.fromCallable(() -> {
                    remove(paymentDetailsKey(paymentDetails.getCurrencyCode(),
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
                        .map(p -> retrieve(paymentDetailsKey(c, p)).map(pd ->
                                PaymentDetails.builder()
                                        .currencyCode(c)
                                        .paymentMethod(p)
                                        .paymentDetails(pd).build()))
                        .filter(Optional::isPresent)
                        .map(Optional::get))
                .compose(eventLogger.logObjects("Loaded"));
    }

    public Observable<PaymentDetails> getUpdatedPaymentDetails() {
        return updatedPaymentDetails
                .compose(eventLogger.logObjects("Updated"))
                .share();
    }

    public Observable<PaymentDetails> getRemovedPaymentDetails() {
        return removedPaymentDetails
                .compose(eventLogger.logObjects("Removed"))
                .share();
    }

    public Observable<PaymentDetails> getSelectedPaymentDetails() {
        return selectedPaymentDetails
                .compose(eventLogger.logObjects("Selected"))
                .share();
    }

    private String paymentDetailsKey(CurrencyCode currencyCode,
                                     PaymentMethod paymentMethod) {

        return String.format("%s.%s.%s", PROFILE_PAYMENT_DETAILS, currencyCode.name(),
                paymentMethod.displayName());
    }
}