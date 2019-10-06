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

import com.bytabit.app.core.badge.BadgeManager;
import com.bytabit.app.core.common.AppConfig;
import com.bytabit.app.core.net.TorManager;
import com.bytabit.app.core.offer.OfferManager;
import com.bytabit.app.core.payment.PaymentDetailsManager;
import com.bytabit.app.core.trade.TradeManager;
import com.bytabit.app.core.wallet.WalletManager;

import java.util.concurrent.Executor;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;

@Component
@Singleton
public interface ApplicationComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder appConfig(AppConfig appConfig);

        @BindsInstance
        Builder walletExecutor(@Named("wallet") Executor walletExecutor);

        ApplicationComponent build();
    }

    TorManager torManager();

    OfferManager offerManager();

    WalletManager walletManager();

    PaymentDetailsManager paymentDetailsManager();

    BadgeManager badgeManager();

    TradeManager tradeManager();
}
