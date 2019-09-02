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

package com.bytabit.app.core.payment.model;

public enum PaymentMethod {
    SWISH("Swish", "Full name and phone number"),
    WU("Western Union", "Full name and ID number"),
    MG("Moneygram", "Full name and ID number"),
    SEPA("SEPA (EU)", "Full name and IBAN number");

    PaymentMethod(String displayName, String requiredDetails) {
        this.displayName = displayName;
        this.requiredDetails = requiredDetails;
    }

    private final String displayName;
    private final String requiredDetails;

    public String displayName() {
        return displayName;
    }

    public String requiredDetails() {
        return requiredDetails;
    }
}