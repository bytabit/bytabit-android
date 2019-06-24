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

package com.bytabit.app.core.common;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;
import org.spongycastle.util.encoders.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HashUtils {

    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static Sha256Hash sha256Hash(Object... propertyValues) {
        ByteArrayOutputStream hashedProperties = new ByteArrayOutputStream();
        try {
            for (Object propertyValue : propertyValues) {
                if (propertyValue != null) {
                    if (propertyValue instanceof String) {
                        hashedProperties.write(Sha256Hash.of(((String) propertyValue).getBytes()).getBytes());
                    } else if (propertyValue instanceof Date) {
                        hashedProperties.write(Sha256Hash.of(dateFormat.format((Date) propertyValue).getBytes()).getBytes());
                    } else if (propertyValue instanceof BigDecimal) {
                        hashedProperties.write(Sha256Hash.of(((BigDecimal) propertyValue).toPlainString().getBytes()).getBytes());
                    } else if (propertyValue instanceof Enum) {
                        hashedProperties.write(Sha256Hash.of((propertyValue).toString().getBytes()).getBytes());
                    } else {
                        throw new HashException("Unsupported property value type");
                    }
                }
            }
            return Sha256Hash.of(hashedProperties.toByteArray());
        } catch (IOException ioe) {
            throw new HashException("Unable to create hash.", ioe);
        }
    }

    public static String base58Sha256Hash(Object... propertyValues) {
        return Base58.encode(sha256Hash(propertyValues).getBytes());
    }

    public static String base64Sha256Hash(Object... propertyValues) {
        return new String(Base64.encode(sha256Hash(propertyValues).getBytes()));
    }

}

