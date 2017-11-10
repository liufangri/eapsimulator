/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-30
 */

package com.tp_link.web.eapsimulator.tools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.RSAPrivateKeyStructure;
import org.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import org.springframework.util.Base64Utils;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

public class RSAHelper {
    private static final Log logger = LogFactory.getLog(RSAHelper.class);

    public static byte[] decryptWithPublicKey(final String encryptedData, final String publicKeyStr) {
        byte[] encryptedDataBytes = decryptBASE64(encryptedData);
        byte[] publicKeyBytes = decryptBASE64(publicKeyStr);
        byte[] res = null;
        try {
            RSAPublicKeyStructure keyStructure = new RSAPublicKeyStructure((ASN1Sequence) ASN1Sequence.fromByteArray(publicKeyBytes));
            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(keyStructure.getModulus(), keyStructure.getPublicExponent());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(rsaPublicKeySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            res = cipher.doFinal(encryptedDataBytes);

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return res;
    }

    public static boolean verifySHA1WithPublicKey(final String encryptedData, final String sign, final String publicKeyStr) {
        byte[] encryptedSignBytes = decryptBASE64(sign);
        byte[] encryptedDataBytes = decryptBASE64(encryptedData);
        byte[] publicKeyBytes = decryptBASE64(publicKeyStr);
        boolean ret = false;
        try {
            RSAPublicKeyStructure keyStruct = new RSAPublicKeyStructure((ASN1Sequence) ASN1Sequence.fromByteArray(publicKeyBytes));
            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(keyStruct.getModulus(), keyStruct.getPublicExponent());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(pubKeySpec);

            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(publicKey);
            signature.update(encryptedDataBytes);

            ret = signature.verify(encryptedSignBytes);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return ret;
    }

    public static byte[] decryptWithPrivateKey(final String encryptedData, final String privateKeyStr) {
        byte[] encryptedDataBytes = decryptBASE64(encryptedData);
        byte[] privateKeyStrBytes = decryptBASE64(privateKeyStr);
        byte[] ret = null;
        try {
            ASN1Sequence sequence = (ASN1Sequence) ASN1Sequence.fromByteArray(privateKeyStrBytes);
            RSAPrivateKeyStructure keyStruct = new RSAPrivateKeyStructure(sequence);
            RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(keyStruct.getModulus(), keyStruct.getPrivateExponent());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");

            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            ret = cipher.doFinal(encryptedDataBytes);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return ret;
    }

    public static byte[] encryptWithPublicKey(final byte[] dataBytes, final String publicKeyStr) {
        byte[] res = null;
        try {
            byte[] publicKeyBytes = decryptBASE64(publicKeyStr);
            RSAPublicKeyStructure keyStruct = new RSAPublicKeyStructure((ASN1Sequence) ASN1Sequence.fromByteArray(publicKeyBytes));
            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(keyStruct.getModulus(), keyStruct.getPublicExponent());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            java.security.interfaces.RSAPublicKey publicKey = (java.security.interfaces.RSAPublicKey) keyFactory.generatePublic(pubKeySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            res = cipher.doFinal(dataBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static byte[] encryptWithPrivateKey(final byte[] dataBytes, final String privateKeyStr) {
        byte[] res = null;
        try {
            byte[] privateKeyBytes = privateKeyStr.getBytes();
            ASN1Sequence sequence = (ASN1Sequence) ASN1Sequence.fromByteArray(privateKeyBytes);
            RSAPrivateKeyStructure keyStruct = new RSAPrivateKeyStructure(sequence);
            RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(keyStruct.getModulus(), keyStruct.getPrivateExponent());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            res = cipher.doFinal(dataBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static byte[] decryptBASE64(String base64Str) {
        return Base64Utils.decode(base64Str.getBytes());
    }

    public static byte[] encryptBASE64(byte[] bytes) {
        return Base64Utils.encode(bytes);
    }
}
