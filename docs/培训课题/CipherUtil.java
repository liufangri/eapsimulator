package com.tp_link.eap.net.security;

/*
 * Copyright (C) 2014 TP-LINK Technologies Co., Ltd. All rights reserved.
 * 
 * Filename: Coder.java
 */

/**
 * @author Tyrian
 * 
 */

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

@SuppressWarnings("restriction")
public class CipherUtil {
    
    public static final String        KEY_SHA       = "SHA";
    public static final String        KEY_MD5       = "MD5";
    public static final BASE64Decoder base64Decoder = new BASE64Decoder();
    public static final BASE64Encoder base64Encoder = new BASE64Encoder();
    
    // must add synchronized for thread safe!!!!!!!!!!!!!!!!
    public static synchronized byte[] decryptBASE64(String key) throws IOException {
        return base64Decoder.decodeBuffer(key);
    }
    
    // must add synchronized for thread safe!!!!!!!!!!!!!!!!
    public static synchronized String encryptBASE64(byte[] key) {
        return base64Encoder.encodeBuffer(key);
    }
    
    public static byte[] encryptMD5(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance(KEY_MD5);
        md5.update(data);
        
        return md5.digest();
    }
    
    public static byte[] encryptSHA(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance(KEY_SHA);
        sha.update(data);
        
        return sha.digest();
    }
    
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString().toUpperCase();
    }
    
}
