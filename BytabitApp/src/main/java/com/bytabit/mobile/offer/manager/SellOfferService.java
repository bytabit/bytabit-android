package com.bytabit.mobile.offer.manager;

import com.bytabit.mobile.common.RetrofitService;
import com.bytabit.mobile.offer.model.SellOffer;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class SellOfferService extends RetrofitService {

    private final SellOfferServiceApi sellOfferServiceApi;

    public SellOfferService() {

        // create an instance of the ApiService
        sellOfferServiceApi = retrofit.create(SellOfferServiceApi.class);
    }

    Single<SellOffer> put(SellOffer sellOffer) {
        return sellOfferServiceApi.put(sellOffer.getSellerEscrowPubKey(), sellOffer)
                .doOnError(t -> log.error("put error: {}", t.getMessage()));
    }

    Single<List<SellOffer>> getAll() {
        return sellOfferServiceApi.get()
                .doOnError(t -> log.error("get error: {}", t.getMessage()));
    }

    Single<SellOffer> delete(String sellerEscrowPubKey) {

        return sellOfferServiceApi.delete(sellerEscrowPubKey)
                .doOnError(t -> log.error("delete error: {}", t.getMessage()));
    }
}
