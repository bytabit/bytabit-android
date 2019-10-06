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

package com.bytabit.app.core.trade;

import com.bytabit.app.core.common.AppConfig;
import com.bytabit.app.core.common.CryptoUtils;
import com.bytabit.app.core.common.CryptoUtilsException;
import com.bytabit.app.core.common.RetryWithDelay;
import com.bytabit.app.core.net.RetrofitService;
import com.bytabit.app.core.trade.model.SignedTrade;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.core.trade.model.TradeModelException;
import com.bytabit.app.core.trade.model.TradeServiceResource;
import com.bytabit.app.core.wallet.WalletManager;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class TradeService extends RetrofitService {

    private final WalletManager walletManager;
    private final TradeServiceApi tradeServiceApi;
    private final CryptoUtils cryptoUtils;

    @Inject
    public TradeService(AppConfig appConfig, WalletManager walletManager, CryptoUtils cryptoUtils) {
        super(appConfig);
        this.walletManager = walletManager;
        this.cryptoUtils = cryptoUtils;

        // create an instance of the ApiService
        this.tradeServiceApi = retrofit.create(TradeServiceApi.class);
    }

    Single<SignedTrade> put(Trade trade) {

        Single<SignedTrade> signedTrade = signTrade(trade).cache();
        Observable<TradeServiceResource> tradeServiceResources = signedTrade
                .flatMapObservable(st -> getReceiverPubKeys(st).map(pk -> toTradeServiceResource(st, pk)));

        return tradeServiceResources.flatMapSingle(tsr -> tradeServiceApi.put(tsr.getId(), tsr)
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("put error: {}", t.getMessage())))
                .flatMapSingle(tr -> signedTrade.map(st -> {
                    st.setVersion(tr.getVersion());
                    return st;
                }))
                .doOnNext(st -> {
                    if (st.getVersion() <= trade.getVersion()) {
                        throw new TradeException("Received trade version less than or equal to trade sent.");
                    }
                })
                .lastOrError();
    }

    private Observable<String> getReceiverPubKeys(SignedTrade signedTrade) {

        return walletManager.getProfilePubKeyBase58().flatMapObservable(myProfilePubKey -> {
            List<String> receiverPubKeys = new ArrayList<>();
            if (signedTrade.getMakerProfilePubKey().equals(myProfilePubKey)) {
                receiverPubKeys.add(signedTrade.getTakerProfilePubKey());
            } else {
                receiverPubKeys.add(signedTrade.getMakerProfilePubKey());
            }
            if (signedTrade.hasArbitrateRequest()) {
                receiverPubKeys.add(signedTrade.getArbitratorProfilePubKey());
            }
            return Observable.fromIterable(receiverPubKeys);
        });
    }

    Single<List<SignedTrade>> getByOfferId(String offerId, Long version) {

        return tradeServiceApi.getByOfferId(offerId, version)
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("get error: {}", t.getMessage()))
                .flatMap(l -> Observable.fromIterable(l)
                        .flatMapMaybe(tsr -> walletManager.getProfileECKey()
                                .map(eckey -> toSignedTrade(tsr, eckey))
                                .onErrorResumeNext(t -> {
                                    if (t instanceof CryptoUtilsException) {
                                        return Maybe.empty();
                                    } else {
                                        return Maybe.error(t);
                                    }
                                }))
                        .filter(this::validateSignedTradeSignature)
                        .toList());
    }

    Single<List<SignedTrade>> get(String id, Long version) {

        return tradeServiceApi.get(id, version)
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("get error: {}", t.getMessage()))
                .flatMap(l -> Observable.fromIterable(l)
                        .flatMapMaybe(tsr -> walletManager.getProfileECKey()
                                .map(eckey -> toSignedTrade(tsr, eckey))
                                .onErrorResumeNext(t -> {
                                    if (t instanceof CryptoUtilsException) {
                                        return Maybe.empty();
                                    } else {
                                        return Maybe.error(t);
                                    }
                                }))
                        .filter(this::validateSignedTradeSignature)
                        .toList());
    }

    Single<List<SignedTrade>> get(Set<String> ids, Long version) {

        return Observable.fromIterable(ids).flatMap(id -> tradeServiceApi.get(id, version)
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("get error: {}", t.getMessage()))
                .flatMapObservable(l -> Observable.fromIterable(l)
                        .flatMapMaybe(tsr -> walletManager.getProfileECKey().map(eckey -> toSignedTrade(tsr, eckey)))
                ).onErrorResumeNext(t -> {
                    if (t instanceof CryptoUtilsException) {
                        return Observable.empty();
                    } else {
                        return Observable.error(t);
                    }
                }))
                .filter(this::validateSignedTradeSignature)
                //.retry(throwable -> throwable instanceof CryptoUtilsException)
                .toList();
    }

    Single<List<SignedTrade>> getArbitrate(Long version) {
        return tradeServiceApi.getArbitrate(version)
                .retryWhen(new RetryWithDelay(5, 2, TimeUnit.SECONDS))
                .doOnError(t -> log.error("get error: {}", t.getMessage()))
                .flatMap(l -> Observable.fromIterable(l)
                        .flatMapMaybe(tsr -> walletManager.getProfileECKey().map(eckey -> toSignedTrade(tsr, eckey)))
                        .onErrorResumeNext(t -> {
                            if (t instanceof CryptoUtilsException) {
                                return Observable.empty();
                            } else {
                                return Observable.error(t);
                            }
                        })
                        .filter(this::validateSignedTradeSignature)
                        .toList());
        //.retry(throwable -> throwable instanceof CryptoUtilsException);
    }

    private TradeServiceResource toTradeServiceResource(SignedTrade signedTrade, String receiverPubKeyBase58) {

        // encrypt signedTrade
        ECKey receiverPubKey = ECKey.fromPublicOnly(Base58.decode(receiverPubKeyBase58));
        String signedTradeJson = gson.toJson(signedTrade);
        String encryptedSignedTrade = cryptoUtils.encrypt(receiverPubKey, signedTradeJson);

        TradeServiceResource tradeServiceResource;
        if (signedTrade.hasTakeOfferRequest() && !signedTrade.hasAcceptance()) {
            tradeServiceResource = TradeServiceResource.builder()
                    .id(UUID.randomUUID().toString())
                    .offerId(signedTrade.getOffer().getId())
                    .trade(encryptedSignedTrade)
                    .tradeUnencrypted(isRegtest ? signedTrade : null)
                    .build();
        } else {
            tradeServiceResource = TradeServiceResource.builder()
                    .id(signedTrade.getId())
                    .arbitrate(signedTrade.hasArbitrateRequest())
                    .trade(encryptedSignedTrade)
                    .tradeUnencrypted(isRegtest ? signedTrade : null)
                    .build();
        }
        return tradeServiceResource;
    }

    private SignedTrade toSignedTrade(TradeServiceResource tradeServiceResource, ECKey profileECKey) {

        // decrypt signedTrade
        try {

            String signedTradeJson = cryptoUtils.decrypt(profileECKey, tradeServiceResource.getTrade());
            SignedTrade signedTrade = gson.fromJson(signedTradeJson, SignedTrade.class);

            signedTrade.setVersion(tradeServiceResource.getVersion());
            return signedTrade;

        } catch (CryptoUtilsException e) {
            //log.debug("Couldn't decrypt id: {}, version: {}", tradeServiceResource.getId(), tradeServiceResource.getVersion());
            throw e;
        }
    }

    private Single<SignedTrade> signTrade(Trade trade) {
        Sha256Hash hash = trade.sha256Hash();
        Single<String> signature = walletManager.getBase58PubKeySignature(hash);

        return signature.map(s -> createSignedTrade(trade, s));
    }

    private SignedTrade createSignedTrade(Trade trade, String signature) {
        return SignedTrade.signedBuilder()
                .id(trade.getId())
                .version(trade.getVersion())
                .status(trade.getStatus())
                .role(trade.getRole())
                .createdTimestamp(trade.getCreatedTimestamp())
                .offer(trade.getOffer())
                .tradeRequest(trade.getTradeRequest())
                .tradeAcceptance(trade.getTradeAcceptance())
                .fundingTransactionWithAmt(trade.getFundingTransactionWithAmt())
                .paymentRequest(trade.getPaymentRequest())
                .payoutRequest(trade.getPayoutRequest())
                .arbitrateRequest(trade.getArbitrateRequest())
                .payoutTransactionWithAmt(trade.getPayoutTransactionWithAmt())
                .cancelCompleted(trade.getCancelCompleted())
                .payoutCompleted(trade.getPayoutCompleted())
                .signature(signature)
                .build();
    }

    private boolean validateSignedTradeSignature(SignedTrade signedTrade) {
        Sha256Hash hash = signedTrade.sha256Hash();

        boolean isValid = walletManager.validateBase58PubKeySignature(getSignerPubKey(signedTrade),
                signedTrade.getSignature(), hash);

        if (!isValid) {
            log.warn("Invalid signature for trade: {}, version: {}", signedTrade.getId(), signedTrade.getVersion());
        }
        return isValid;
    }

    private String getSignerPubKey(SignedTrade signedTrade) {

        String signerProfilePubKey = null;

        if (signedTrade.hasTakeOfferRequest()) {
            signerProfilePubKey = signedTrade.getTakerProfilePubKey();
        }
        if (signedTrade.hasAcceptance()) {
            signerProfilePubKey = signedTrade.getMakerProfilePubKey();
        }
        if (signedTrade.hasPaymentRequest()) {
            signerProfilePubKey = signedTrade.getSellerProfilePubKey();
        }
        if (signedTrade.hasPayoutRequest()) {
            signerProfilePubKey = signedTrade.getBuyerProfilePubKey();
        }
        if (signedTrade.hasArbitrateRequest()) {
            switch (signedTrade.getArbitrateRequest().getReason()) {
                case NO_BTC:
                    signerProfilePubKey = signedTrade.getBuyerProfilePubKey();
                    break;
                case NO_PAYMENT:
                    signerProfilePubKey = signedTrade.getSellerProfilePubKey();
                    break;
            }
        }
        if (signedTrade.hasPayoutCompleted()) {
            switch (signedTrade.getPayoutCompleted().getReason()) {
                case SELLER_BUYER_PAYOUT:
                    signerProfilePubKey = signedTrade.getSellerProfilePubKey();
                    break;
                case BUYER_SELLER_REFUND:
                    signerProfilePubKey = signedTrade.getBuyerProfilePubKey();
                    break;
                case ARBITRATOR_BUYER_PAYOUT:
                case ARBITRATOR_SELLER_REFUND:
                    signerProfilePubKey = signedTrade.getArbitratorProfilePubKey();
                    break;
            }
        }
        if (signedTrade.hasCancelCompleted()) {
            switch (signedTrade.getCancelCompleted().getReason()) {
                case BUYER_CANCEL_FUNDED:
                case BUYER_CANCEL_UNFUNDED:
                    signerProfilePubKey = signedTrade.getBuyerProfilePubKey();
                    break;
                case SELLER_CANCEL_UNFUNDED:
                    signerProfilePubKey = signedTrade.getSellerProfilePubKey();
                    break;
            }
        }

        if (signerProfilePubKey == null) {
            throw new TradeModelException("Unable to determine trade signer.");
        }

        return signerProfilePubKey;
    }
}
