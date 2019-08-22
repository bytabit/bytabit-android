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
import org.spongycastle.jcajce.provider.util.BadBlockException;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.math.ec.ECPoint;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
    public void whenBitcoinjECKeys_encryptMessage_returnSameMessage() throws NoSuchProviderException,
            NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Security.addProvider(new BouncyCastleProvider());

        ECKey aECKey = new ECKey();
        ECKey bECKey = new ECKey();
        ECKey bPubECKey = ECKey.fromPublicOnly(bECKey.getPubKeyPoint());

        String aClearText = "hello world -- a nice day today";
        log.debug("aClearText: {}", aClearText);

        String bCypherText = cryptoUtils.encrypt(bPubECKey, aClearText);
        log.debug("bCypherText: {}", bCypherText);

        String bClearText = cryptoUtils.decrypt(bECKey, bCypherText);
        log.debug("bClearText: {}", new String(bClearText));

        assert (aClearText.equals(bClearText));

        try {
            cryptoUtils.decrypt(aECKey, bCypherText);
            assert (false);
        } catch (BadBlockException bbe) {
            assert (true);
        }
    }

}