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

package com.bytabit.app;

import com.bytabit.app.core.common.AppConfig;
import com.bytabit.app.core.offer.model.Offer;
import com.bytabit.app.core.payment.model.CurrencyCode;
import com.bytabit.app.core.payment.model.PaymentMethod;
import com.bytabit.app.core.trade.manager.TradeStorage;
import com.bytabit.app.core.trade.model.PaymentRequest;
import com.bytabit.app.core.trade.model.PayoutRequest;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.core.trade.model.TradeAcceptance;
import com.bytabit.app.core.trade.model.TradeRequest;

import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@Slf4j
public class TestTradeStorage {

    private AppConfig appConfig = AppConfig.builder()
            .configName("unittest")
            .btcNetwork("regtest")
            .peerAddress("null")
            .peerPort("null")
            .appStorage(new File("/tmp/bytabit/"))
            .build();

    private TradeStorage tradeStorage = new TradeStorage(appConfig);

    @Test
    public void whenSavedTrade_returnSameTrade() {

        Date createdTimestamp = new Date();

        Offer offer = Offer.builder()
                .id(UUID.randomUUID().toString())
                .offerType(Offer.OfferType.SELL)
                .makerProfilePubKey("testMakerProfilePubKey")
                .currencyCode(CurrencyCode.SEK)
                .paymentMethod(PaymentMethod.SWISH)
                .minAmount(BigDecimal.valueOf(100.00))
                .maxAmount(BigDecimal.valueOf(1000.00))
                .price(BigDecimal.valueOf(123000.00))
                .build();

        TradeRequest tradeRequest = TradeRequest.builder()
                .takerProfilePubKey("testTakerProfilePubKey")
                .takerEscrowPubKey("testTakerEscrowPubKey")
                .btcAmount(BigDecimal.valueOf(.10))
                .paymentAmount(BigDecimal.valueOf(123000.00 * .10))
                .build();

        TradeAcceptance tradeAcceptance = TradeAcceptance.builder()
                .makerEscrowPubKey("testEscrowPubKey")
                .arbitratorProfilePubKey("testArbitratorProfilePubKey")
                .escrowAddress("testEscrowAddress")
                .build();

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .fundingTxHash("testFundingTxHash")
                .paymentDetails("testPaymentDetails")
                .refundAddress("testRefundAddress")
                .refundTxSignature("testRefundTxSignature")
                .txFeePerKb(BigDecimal.ONE)
                .build();

        //TransactionWithAmt fundingTx = TransactionWithAmt.builder().build();

        PayoutRequest payoutRequest = PayoutRequest.builder()
                .paymentReference("testPaymentReference")
                .payoutAddress("testPayoutAddress")
                .payoutTxSignature("testPayoutTsSignature")
                .build();

        //TransactionWithAmt payoutTx = TransactionWithAmt.builder().build();

        Trade trade = Trade.builder()
                .id("abc123")
                .createdTimestamp(createdTimestamp)
                .offer(offer)
                .tradeRequest(tradeRequest)
                .tradeAcceptance(tradeAcceptance)
                .paymentRequest(paymentRequest)
                //.fundingTransactionWithAmt(fundingTx)
                .payoutRequest(payoutRequest)
                //.payoutTransactionWithAmt(payoutTx)
                .build();

        Single<Trade> writtenTrade = tradeStorage.write(trade);

        writtenTrade.flatMapMaybe(st -> tradeStorage.read(st.getId())).subscribe(rt -> {
            assert (trade.equals(rt));
            log.debug("Written trade: {}", trade);
            log.debug("Read trade: {}", rt);
        });
    }
}