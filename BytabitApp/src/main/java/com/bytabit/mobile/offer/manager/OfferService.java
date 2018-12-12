package com.bytabit.mobile.offer.manager;

import com.bytabit.mobile.common.RetrofitService;
import com.bytabit.mobile.offer.model.Offer;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class OfferService extends RetrofitService {

    private final OfferServiceApi offerServiceApi;

    public OfferService() {

        // create an instance of the ApiService
        offerServiceApi = retrofit.create(OfferServiceApi.class);
    }

    Single<Offer> put(Offer sellOffer) {
        return offerServiceApi.put(sellOffer.getId(), sellOffer)
                .doOnError(t -> log.error("put error: {}", t.getMessage()));
    }

    Single<List<Offer>> getAll() {
        return offerServiceApi.get()
                .doOnError(t -> log.error("get error: {}", t.getMessage()));
    }

    Single<Offer> delete(String id) {

        return offerServiceApi.delete(id)
                .doOnError(t -> log.error("delete error: {}", t.getMessage()));
    }
}
