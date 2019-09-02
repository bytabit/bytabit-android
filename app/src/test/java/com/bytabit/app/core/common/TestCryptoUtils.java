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

import org.bitcoinj.core.ECKey;
import org.junit.Test;
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.math.ec.ECPoint;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;

import lombok.extern.slf4j.Slf4j;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@Slf4j
public class TestCryptoUtils {

    private final CryptoUtils cryptoUtils;

    public TestCryptoUtils() throws NoSuchProviderException, NoSuchAlgorithmException {
        this.cryptoUtils = new CryptoUtils();
    }

    private KeyPair toKeyPair(ECKey ecKey) throws InvalidKeySpecException {

        PrivateKey privateKey = cryptoUtils.toPrivateKey(ecKey);
        PublicKey publicKey = cryptoUtils.toPublicKey(ecKey);

        return new KeyPair(publicKey, privateKey);
    }


    @Test
    public void whenBitcoinjECKey_returnSameBcECKey() throws InvalidKeySpecException {

        ECParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");

        // test ECKey
        ECKey ecKey1 = new ECKey().decompress();
        log.debug("ECKey1: {}", ecKey1);

        KeyPair keyPair = toKeyPair(ecKey1);

        BCECPublicKey ecPublicKey = (BCECPublicKey) keyPair.getPublic();
        BCECPrivateKey ecPrivateKey = (BCECPrivateKey) keyPair.getPrivate();

        ECPoint ecPoint = spec.getCurve().createPoint(ecPublicKey.getW().getAffineX(), ecPublicKey.getW().getAffineY());
        ECKey ecKey2 = ECKey.fromPrivateAndPrecalculatedPublic(ecPrivateKey.getD(), ecPoint);
        log.debug("ECKey2: {}", ecKey2);

        assert (ecKey1.getPrivKey().equals(ecKey2.getPrivKey()));
        assert (ecKey1.getPubKeyPoint().equals(ecKey2.getPubKeyPoint()));
    }

    @Test
    public void whenBitcoinjECKeys_encryptMessage_returnSameMessage() {

        Security.addProvider(new BouncyCastleProvider());

        ECKey aECKey = new ECKey();
        ECKey bECKey = new ECKey();
        ECKey bPubECKey = ECKey.fromPublicOnly(bECKey.getPubKeyPoint());

        String aClearText = "{\n" +
                "  \"signature\": \"AN1rKvtJY7qswET68zt5VJpuDSmH5dBX1FfwDfDSt9M9Xbr2p2RoeXEzbm7ke7TxVD7XFJxAwQEYyKj4VVnSsNBqbLUtEftxz\",\n" +
                "  \"createdTimestamp\": \"2019-08-28T14:36:28.463+0000\",\n" +
                "  \"id\": \"994e2562-41a8-4225-886a-274e9c2b4fc8\",\n" +
                "  \"offer\": {\n" +
                "    \"signature\": \"381yXYromHR8B7e9pq33hSmXxMu4iQCgBn6vJ3um5rnm3LAaSbYp8VusxY761QffA3ZE3empQb2QsYKvtuo7gZ43ohSF2H5W\",\n" +
                "    \"currencyCode\": \"SEK\",\n" +
                "    \"id\": \"6df0bfde-c9c4-49ef-b7a9-3666edecd0ed\",\n" +
                "    \"makerProfilePubKey\": \"cCRytdt5cWyPECc4UGqmEPLsJMhzxeDtwosY6AUgxawM\",\n" +
                "    \"maxAmount\": 1000,\n" +
                "    \"minAmount\": 100,\n" +
                "    \"offerType\": \"SELL\",\n" +
                "    \"paymentMethod\": \"SWISH\",\n" +
                "    \"price\": 123000\n" +
                "  },\n" +
                "  \"role\": \"BUYER\",\n" +
                "  \"status\": \"CREATED\",\n" +
                "  \"tradeRequest\": {\n" +
                "    \"btcAmount\": 0.00270732,\n" +
                "    \"paymentAmount\": 334,\n" +
                "    \"takerEscrowPubKey\": \"mwqtH1bje74A1AgmFA8z4YtmZrofcNk1zTBXb8HQF6Vh\",\n" +
                "    \"takerProfilePubKey\": \"22bkkTP1a96keNnANfj8jM26o8QUrABsEmTVQLBVYDRUE\"\n" +
                "  },\n" +
                "  \"version\": 0\n" +
                "}";

        log.debug("aClearText: {}", aClearText);

        String bCypherText = cryptoUtils.encrypt(bPubECKey, aClearText);
        log.debug("bCypherText: {}", bCypherText);

        String bClearText = cryptoUtils.decrypt(bECKey, bCypherText);
        log.debug("bClearText: {}", new String(bClearText));

        assert (aClearText.equals(bClearText));

        try {
            cryptoUtils.decrypt(aECKey, bCypherText);
            assert (false);
        } catch (CryptoUtilsException cue) {
            assert (cue.getCause() instanceof BadPaddingException);
        }
    }

}