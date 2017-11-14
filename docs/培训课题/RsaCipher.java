package com.tp_link.eap.net.security;

/*
 * Copyright (C) 2014 TP-LINK Technologies Co., Ltd. All rights reserved.
 * 
 * Filename: RSACoder.java
 */

/**
 * @author Tyrian
 * 
 */

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import org.bouncycastle.asn1.ASN1Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tp_link.eap.configuration.Configuration;
import com.tp_link.eap.configuration.ConfigurationFactory;
import com.tp_link.eap.configuration.PropertyKey;
import com.tp_link.eap.constants.GlobalConfig;
import com.tp_link.eap.net.exception.EapNetException;

public abstract class RsaCipher {
    public static final String     KEY_ALGORITHM       = "RSA";
    public static final String     SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static PrivateKey      PRIVATE_KEY;
    private static PublicKey       PUBLIC_KEY;
    private static final Logger    logger              = LoggerFactory.getLogger(RsaCipher.class);
    
    public static final Object     _lock               = new Object();
    public static volatile boolean initilized          = false;
    
    public static String sign(byte[] data) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(PRIVATE_KEY);
            signature.update(data);
            
            return CipherUtil.encryptBASE64(signature.sign());
        } catch (Exception e) {
            throw new EapNetException("sign failed", e.getCause());
        }
    }
    
    public static boolean verify(byte[] data, String sign) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(PUBLIC_KEY);
            signature.update(data);
            
            return signature.verify(CipherUtil.decryptBASE64(sign));
        } catch (Exception e) {
            logger.warn("failed", e);
            throw new EapNetException("verify failed", e.getCause());
        }
    }
    
    public static byte[] decryptByPrivateKey(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, PRIVATE_KEY);
            
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new EapNetException("decrypt by private key failed", e.getCause());
        }
    }
    
    public static byte[] decryptByPublicKey(byte[] data, String key) {
        try {
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, PUBLIC_KEY);
            
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new EapNetException("decrypt by public key failed", e.getCause());
        }
    }
    
    public static byte[] encryptByPublicKey(byte[] data, String key) {
        try {
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, PUBLIC_KEY);
            
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new EapNetException("encrypt by public key failed", e.getCause());
        }
    }
    
    private static void generatePrivateKey(String key) {
        try {
            byte[] keyBytes = CipherUtil.decryptBASE64(key);
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PRIVATE_KEY = keyFactory.generatePrivate(pkcs8KeySpec);
        } catch (Exception e) {
            logger.warn("generate private key error .", e);
            throw new EapNetException(e);
        }
    }
    
    private static void generatePublicKey(String key) {
        try {
            byte[] keyBytes = CipherUtil.decryptBASE64(key);
            
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PUBLIC_KEY = keyFactory.generatePublic(x509KeySpec);
        } catch (Exception e) {
            logger.warn("generate public key error .", e);
            throw new EapNetException(e);
        }
    }
    
    public static byte[] encryptByPrivateKey(byte[] data) {
        try {
            
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, PRIVATE_KEY);
            
            return cipher.doFinal(data);
        } catch (Exception e) {
            logger.warn("encrypt by private key failed", e);
            throw new EapNetException("encrypt by private key failed", e.getCause());
        }
    }
    
    /**
     * 将openssl生成的密钥转换为Java可用
     * 
     * @param privateKeyStr
     * @return
     * @throws Exception
     */
    private static String loadPrivateKey(String privateKeyStr) {
        try {
            byte[] buffer = CipherUtil.decryptBASE64(privateKeyStr);
            org.bouncycastle.asn1.pkcs.RSAPrivateKey asn1PrivKey = org.bouncycastle.asn1.pkcs.RSAPrivateKey
                .getInstance(ASN1Sequence.fromByteArray(buffer));
            RSAPrivateKeySpec rsaPrivKeySpec = new RSAPrivateKeySpec(asn1PrivKey.getModulus(),
                asn1PrivKey.getPrivateExponent());
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(rsaPrivKeySpec);
            return CipherUtil.encryptBASE64(privateKey.getEncoded());
            
        } catch (Exception e) {
            logger.error("Load private key failed.", e);
            throw new EapNetException("Load private key failed.", e.getCause());
        }
    }
    
    private static String loadPublicKey(String publicKeyStr) {
        try {
            byte[] buffer = CipherUtil.decryptBASE64(publicKeyStr);
            org.bouncycastle.asn1.pkcs.RSAPublicKey asn1PublicKey = org.bouncycastle.asn1.pkcs.RSAPublicKey
                .getInstance((ASN1Sequence) ASN1Sequence.fromByteArray(buffer));
            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(asn1PublicKey.getModulus(),
                asn1PublicKey.getPublicExponent());
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(rsaPublicKeySpec);
            return CipherUtil.encryptBASE64(publicKey.getEncoded());
            
        } catch (Exception e) {
            logger.error("Load public key failed.", e);
            throw new EapNetException("Load public key failed.", e.getCause());
        }
    }
    
    public static void initilize() {
        if (!initilized) {
            synchronized (_lock) {
                if (!initilized) {
                    String prikey = loadPrivateKey(GlobalConfig.NETTY_RSA_PRIVATE_KEY);
                    generatePrivateKey(prikey);
                    
                }
                initilized = true;
            }
        }
        
    }
    
    public static PrivateKey getPrivateKey() {
        try {
            initilize();
        } catch (Exception e) {
            logger.debug("exception .", e);
        }
        return PRIVATE_KEY;
    }
    
    public static PublicKey getPublicKey() {
        try {
            initilize();
        } catch (Exception e) {
            logger.debug("exception .", e);
        }
        return PUBLIC_KEY;
    }
    
}