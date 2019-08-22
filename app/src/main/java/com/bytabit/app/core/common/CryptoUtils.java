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
import org.bitcoinj.core.ECKey;
import org.spongycastle.jcajce.provider.util.BadBlockException;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPrivateKeySpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;
import org.spongycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CryptoUtils {

    private final ECParameterSpec spec;
    private final KeyFactory keyFactory;

    public CryptoUtils() throws NoSuchProviderException, NoSuchAlgorithmException {

        Security.addProvider(new BouncyCastleProvider());
        this.spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        this.keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
    }

    public String encrypt(ECKey receiverPubKey, String clearText) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        PublicKey publicKey = toPublicKey(receiverPubKey);

        Cipher ecIESCipher = Cipher.getInstance("ECIES", BouncyCastleProvider.PROVIDER_NAME);
        ecIESCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] clearTextData = clearText.getBytes();
        byte[] cipherData = ecIESCipher.doFinal(clearTextData, 0, clearTextData.length);
        return Base58.encode(cipherData);
    }

    public String decrypt(ECKey receiverPrvKey, String cypherTextBase58) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        Cipher ecIESCipher = Cipher.getInstance("ECIES", BouncyCastleProvider.PROVIDER_NAME);
        ecIESCipher.init(Cipher.DECRYPT_MODE, toPrivateKey(receiverPrvKey));

        try {
            byte[] clearTextData = ecIESCipher.doFinal(Base58.decode(cypherTextBase58));
            return new String(clearTextData);
        } catch (BadBlockException bbe) {
            log.debug("Couldn't decrypt: {}", bbe.getMessage());
            throw bbe;
        }
    }

    PublicKey toPublicKey(ECKey ecKey) throws InvalidKeySpecException {

        // Q
        ECPoint q = ecKey.getPubKeyPoint();
        ECPublicKeySpec pubKey = new ECPublicKeySpec(q, spec);
        return keyFactory.generatePublic(pubKey);
    }

    PrivateKey toPrivateKey(ECKey ecKey) throws InvalidKeySpecException {

        // d
        BigInteger d = ecKey.getPrivKey();
        ECPrivateKeySpec prvkey = new ECPrivateKeySpec(d, spec);
        return keyFactory.generatePrivate(prvkey);
    }
}
