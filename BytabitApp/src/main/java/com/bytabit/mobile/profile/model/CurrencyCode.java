/*
 * Copyright 2018 Bytabit AB
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

package com.bytabit.mobile.profile.model;

import java.util.Arrays;
import java.util.List;

public enum CurrencyCode {

    SEK(0, PaymentMethod.SWISH, PaymentMethod.MG, PaymentMethod.WU),
    USD(2, PaymentMethod.MG, PaymentMethod.WU);

    CurrencyCode(int scale, PaymentMethod... paymentMethods) {
        this.scale = scale;
        this.paymentMethods = Arrays.asList(paymentMethods);
    }

    private int scale;

    private List<PaymentMethod> paymentMethods;

    public int getScale() {
        return scale;
    }

    public List<PaymentMethod> paymentMethods() {
        return paymentMethods;
    }
}
