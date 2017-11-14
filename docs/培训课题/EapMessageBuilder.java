/*
 * Copyright (C) 2014 TP-LINK Technologies Co., Ltd. All rights reserved.
 * 
 * Project: com.tp-link.eap
 */

package com.tp_link.eap.net.util;

import com.tp_link.eap.domain.message.body.AuthorizationBody;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tp_link.eap.device.AbstractDevice;
import com.tp_link.eap.device.ActiveDevice;
import com.tp_link.eap.domain.message.EapMessage;
import com.tp_link.eap.domain.message.EapMessageType;
import com.tp_link.eap.domain.message.ErrorCode;
import com.tp_link.eap.domain.message.body.AdoptRequestBody;
import com.tp_link.eap.domain.message.body.EapConfigBody;
import com.tp_link.eap.domain.message.body.EapUpgradeBody;
import com.tp_link.eap.domain.message.header.EapHeader;
import com.tp_link.eap.net.constants.TransportConfig;
import com.tp_link.eap.net.exception.EapNetException;
import com.tp_link.eap.net.security.CipherUtil;
import com.tp_link.eap.net.security.RsaCipher;

/**
 * @author Tyrian 2014/9/24
 */

public class EapMessageBuilder {
    private static final Logger logger = LoggerFactory.getLogger(EapMessageBuilder.class);
    
    public static EapMessage buildAdoptRequest(AbstractDevice device) throws EapNetException {
        EapMessage msg = new EapMessage();
        EapHeader header = new EapHeader();
        AdoptRequestBody body = new AdoptRequestBody();
        
        header.setVersion(TransportConfig.EAP_PROTO_VERSION);
        header.setMac(device.getDeviceId());
        header.setType(EapMessageType.ADOPT_REQUEST.getIntValue());
        header.setError(ErrorCode.SUCCESS);
        
        String authData;
        String hashData;
        String signData;
        
        try {
            String passwordMd5 = CipherUtil.bytesToHexString(CipherUtil.encryptMD5(device
                .getPassword().getBytes(CharsetUtil.UTF_8)));
            byte[] usernameBytes = device.getUsername().getBytes(CharsetUtil.UTF_8);
            byte[] nullBytes = { 0 };
            byte[] passwordBytes = passwordMd5.getBytes(CharsetUtil.UTF_8);
            int size = usernameBytes.length + passwordBytes.length + nullBytes.length;
            byte[] authBytes = new byte[size];
            System.arraycopy(usernameBytes, 0, authBytes, 0, usernameBytes.length);
            System.arraycopy(nullBytes, 0, authBytes, usernameBytes.length, nullBytes.length);
            System.arraycopy(passwordBytes, 0, authBytes, usernameBytes.length + nullBytes.length,
                passwordBytes.length);
            byte[] encodedAuth = RsaCipher.encryptByPrivateKey(authBytes);
            authData = CipherUtil.encryptBASE64(encodedAuth);
            hashData = CipherUtil.encryptBASE64(CipherUtil.encryptSHA(encodedAuth));
            signData = RsaCipher.sign(encodedAuth);
            
        } catch (Exception e) {
            logger.warn("Build adopt request failed for mac: " + device.getDeviceId(), e);
            throw new EapNetException(
                "Build adopt request failed for mac: " + device.getDeviceId(), e);
        }
        
        body.setAuth(authData);
        body.setHash(hashData);
        body.setSign(signData);
        msg.setHeader(header);
        msg.setBody(body);
        
        // logger.debug(JsonUtil.bean2Json(msg));
        
        return msg;
    }
}
