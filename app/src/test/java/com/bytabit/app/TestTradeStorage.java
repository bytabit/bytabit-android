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
import com.bytabit.app.core.trade.manager.TradeStorage;
import com.bytabit.app.core.trade.model.PaymentRequest;
import com.bytabit.app.core.trade.model.PayoutRequest;
import com.bytabit.app.core.trade.model.Trade;
import com.bytabit.app.core.trade.model.TradeAcceptance;
import com.bytabit.app.core.trade.model.TradeRequest;
import com.bytabit.app.core.wallet.model.TransactionWithAmt;

import org.junit.Test;

import java.io.File;
import java.util.Date;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
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

        Offer offer = Offer.builder().build();

        TradeRequest tradeRequest = TradeRequest.builder().build();

        TradeAcceptance tradeAcceptance = TradeAcceptance.builder().build();

        PaymentRequest paymentRequest = PaymentRequest.builder().build();

        TransactionWithAmt fundingTx = TransactionWithAmt.builder().build();

        PayoutRequest payoutRequest = PayoutRequest.builder().build();

        TransactionWithAmt payoutTx = TransactionWithAmt.builder().build();

        Trade trade = Trade.builder()
                .id("abc123")
                .createdTimestamp(createdTimestamp)
                .offer(offer)
                .tradeRequest(tradeRequest)
                .tradeAcceptance(tradeAcceptance)
                .paymentRequest(paymentRequest)
                .fundingTransactionWithAmt(fundingTx)
                .payoutRequest(payoutRequest)
                .payoutTransactionWithAmt(payoutTx)
                .build();

        tradeStorage.write(trade);
    }
}