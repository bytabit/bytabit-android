package com.bytabit.mobile.offer;

import com.bytabit.mobile.common.AbstractManager;
import com.bytabit.mobile.offer.model.Offer;
import com.bytabit.mobile.profile.model.CurrencyCode;
import com.bytabit.mobile.profile.model.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;

public class OfferManager extends AbstractManager {

    private static Logger LOG = LoggerFactory.getLogger(OfferManager.class);

    private final OfferService offerService;

    public OfferManager() {
        super();
        offerService = retrofit.create(OfferService.class);
    }

    public Offer createOffer(String pubKey, String sellerPubKey, CurrencyCode currencyCode,
                             PaymentMethod paymentMethod, BigDecimal minAmount,
                             BigDecimal maxAmount, BigDecimal price) {

        Offer offer = null;
        try {
            offer = offerService.createOffer(new Offer(pubKey, sellerPubKey, currencyCode,
                    paymentMethod, minAmount, maxAmount, price)).execute().body();
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
        }
        return offer;
    }
}