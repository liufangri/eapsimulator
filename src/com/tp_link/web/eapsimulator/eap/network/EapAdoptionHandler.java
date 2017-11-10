/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-28
 */

package com.tp_link.web.eapsimulator.eap.network;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.tp_link.web.eapsimulator.eap.VirtualEap;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataType;
import com.tp_link.web.eapsimulator.eap.network.protocol.Packet;
import com.tp_link.web.eapsimulator.tools.GsonHelper;
import com.tp_link.web.eapsimulator.tools.RC4Helper;
import com.tp_link.web.eapsimulator.tools.RSAHelper;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataBody;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataHeader;
import com.tp_link.web.eapsimulator.tools.TypeConvert;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;

@ChannelHandler.Sharable
public class EapAdoptionHandler extends ChannelInboundHandlerAdapter {
    /**
     * The max size of full packet supported.
     */
    private static final Log logger = LogFactory.getLog(EapAdoptionHandler.class);

    static final int MAX_LEN = 2 * 1024 * 1024;

    private EapNetContext context;
    private String publicKeyStr;

    public EapAdoptionHandler() {
    }

    public void setContext(EapNetContext context) {
        this.context = context;
    }

    public void setPublicKeyStr(String publicKeyStr) {
        this.publicKeyStr = publicKeyStr;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        logger.error(throwable);
        if (throwable instanceof IOException) {
            Channel channel = ctx.channel();
            int port = ((InetSocketAddress) channel.localAddress()).getPort();
            VirtualEap virtualEap = context.getAdoptionClient().getEapByPort(port);
            if (virtualEap == null) {
                return;
            }
            if (!channel.isActive()) {
                logger.error("EAP - " + virtualEap.getMac() + "Disconnected from controller!");
            }
            context.doForgetOperation(virtualEap);
            virtualEap.setCurrentState(VirtualEap.State.INIT);
            synchronized (virtualEap.lock) {
                virtualEap.lock.notify();
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        int port = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
        VirtualEap virtualEap = context.getAdoptionClient().getEapByPort(port);
        if (virtualEap == null) {
            // No virtual eap response to this port.
            return;
        }
        processRequest(virtualEap, ctx, byteBuf);
    }

    private boolean processRequest(VirtualEap virtualEap, ChannelHandlerContext ctx, ByteBuf byteBuf) {
        byte[] lengthBytes = new byte[4];
        byteBuf.readBytes(lengthBytes, 0, 4);
        int len = TypeConvert.bytesToInt(lengthBytes, 0);
        String keyStr = virtualEap.getRc4Key();
        if (isRC4Used(virtualEap)) {
            // RC4 is used.
            byte[] key = RSAHelper.decryptBASE64(keyStr);
            RC4Helper.RC4(lengthBytes, lengthBytes, key);
            len = TypeConvert.bytesToInt(lengthBytes, 0);

            if (len <= 0 || len >= MAX_LEN) {
                return false;
            } else {
                if (len == byteBuf.readableBytes()) {
                    byte[] bytes = new byte[byteBuf.readableBytes()];
                    byteBuf.readBytes(bytes);
                    RC4Helper.RC4(bytes, bytes, key);
                    return processData(virtualEap, ctx, bytes);
                } else {
                    return false;
                }
            }
        } else {
            if (len <= 0 || len >= MAX_LEN) {
                return false;
            } else {
                if (len == byteBuf.readableBytes()) {
                    byte[] bytes = new byte[byteBuf.readableBytes()];
                    byteBuf.readBytes(bytes);
                    return processData(virtualEap, ctx, bytes);
                } else {
                    return false;
                }
            }
        }
    }

    private boolean processData(VirtualEap virtualEap, ChannelHandlerContext ctx, byte[] bytes) {
        String data = new String(bytes);
        logger.debug("Received: " + data);
        try {
            Type typeOfT = new TypeToken<Map<String, Map<String, Object>>>() {
            }.getType();
            Map<String, Map<String, Object>> requestDataMap = GsonHelper.getGson().fromJson(data, typeOfT);
            int type = 0;
            try {
                Object o = requestDataMap.get("header").get("type");
                if (o instanceof Double) {
                    type = ((Double) o).intValue();
                } else if (o instanceof String) {
                    type = Integer.parseInt((String) o);
                } else if (o instanceof Integer) {
                    type = (Integer) o;
                }
            } catch (NumberFormatException e) {
                return false;
            }

            context.getNetLog().log(virtualEap, data, "RECEIVE", type, ctx);

            handleData(virtualEap, ctx, requestDataMap);
            return true;
        } catch (JsonParseException e) {
            logger.debug(e.getMessage());
            return false;
        }
    }

    private void handleData(VirtualEap virtualEap, ChannelHandlerContext ctx,
                            Map<String, Map<String, Object>> requestDataMap) {
        // Data is a valid data.
        if (requestDataMap == null || requestDataMap.isEmpty()) {
            return;
        }
        Map<String, Object> headerMap = requestDataMap.get("header");
        if (headerMap == null || headerMap.isEmpty()) {
            return;
        }
        int type = 0;
        try {
            Object o = headerMap.get("type");
            if (o instanceof Double) {
                type = ((Double) o).intValue();
            } else if (o instanceof String) {
                type = Integer.parseInt((String) o);
            } else if (o instanceof Integer) {
                type = (Integer) o;
            }
        } catch (NumberFormatException e) {
            return;
        }
        if (type == DataType.ADOPT_REQUEST) {
            // This is an adopt request.
            if (handleAdoptionRequest(virtualEap, requestDataMap)) {
                sendAdoptResponse(ctx, virtualEap);
                // Set State CONNECTED
                virtualEap.setCurrentState(VirtualEap.State.PROVISIONING);
                synchronized (virtualEap.lock) {
                    virtualEap.lock.notify();
                }
                return;
            }
            // Adopt fail.
            virtualEap.setCurrentState(VirtualEap.State.INIT);
            synchronized (virtualEap.lock) {
                virtualEap.lock.notify();
            }
        } else if (type == DataType.SET_REQUEST) {
            Map<String, Object> body = requestDataMap.get("body");
            int configVersion = -1;
            try {
                Object o = body.get("configVersion");
                if (o instanceof Double) {
                    configVersion = ((Double) o).intValue();
                } else if (o instanceof String) {
                    configVersion = Integer.parseInt((String) o);
                } else if (o instanceof Integer) {
                    configVersion = (Integer) o;
                }
            } catch (NumberFormatException e) {
                return;
            } catch (NullPointerException e) {
                logger.error("Body is null! " + body.toString());
                return;
            }

            if (configVersion == 0) {
                // A pre set.
                virtualEap.setUserName(((Map<String, String>) body.get("userAccount")).get("newUsername"));
                virtualEap.setPasswordMD5(((Map<String, String>) body.get("userAccount")).get("newPassword"));
            }
        }
    }


    /**
     * Send adopt response.
     *
     * @param ctx the ChannelHandlerContext
     */
    private void sendAdoptResponse(ChannelHandlerContext ctx, VirtualEap virtualEap) {
        DataHeader dataHeader = new DataHeader();
        dataHeader.setType(DataType.ADOPT_RESPONSE);
        dataHeader.setMac(virtualEap.getMac());

        DataBody dataBody = new DataBody();

        // Get RC4 Key encrypted with Base64.
        String keyStr = virtualEap.getRc4Key();

        String keyEncrypted;

        if (keyStr == null || "".equals(keyStr)) {
            // Create random RC4 key.
            byte[] key = getRandomRC4Key();
            keyStr = new String(RSAHelper.encryptBASE64(key));
            virtualEap.setRc4Key(keyStr);

            // Encrypt RC4 key with RSA public key
            keyEncrypted = new String(RSAHelper.encryptBASE64(RSAHelper.encryptWithPublicKey(key, publicKeyStr)));
        } else {
            keyEncrypted = new String(RSAHelper.encryptBASE64(RSAHelper.encryptWithPublicKey(
                    RSAHelper.decryptBASE64(keyStr), publicKeyStr)));
        }

        keyEncrypted = splitBase64(keyEncrypted);

        dataBody.put("key", keyEncrypted);
        dataBody.put("configVersion", 0);

        Packet packet = new Packet(dataHeader, dataBody);

        String json = GsonHelper.getGson().toJson(packet.getData());
        byte[] jsonByte = json.getBytes(Charset.forName("UTF-8"));

        ByteBuf byteBuf1 = ctx.alloc().buffer();
        byteBuf1.writeInt(jsonByte.length);
        byteBuf1.writeBytes(jsonByte);

        ctx.writeAndFlush(byteBuf1);
        context.getNetLog().log(virtualEap, json, "SEND", DataType.ADOPT_RESPONSE, ctx);

    }

    /**
     * Create a random RC4 key.
     *
     * @return The key byte array
     */
    private byte[] getRandomRC4Key() {
        byte[] key = new byte[16];
        int len = (Byte.MAX_VALUE - Byte.MIN_VALUE);
        for (int i = 0; i < 16; i++) {
            key[i] = (byte) (Math.random() * len + Byte.MIN_VALUE);
        }
        return key;
    }

    /**
     * Process string, split every 64 chars
     *
     * @param origin The String to be processed
     * @return The String after processed.
     */
    private String splitBase64(String origin) {
        int len = origin.length();
        StringBuilder sb = new StringBuilder();
        int times = len / 64;
        int i = 0;
        while (i < times) {
            sb.append(origin.substring(i * 64, (i + 1) * 64));
            sb.append('\n');
            i++;
        }
        sb.append(origin.substring(i * 64, len));
        sb.append('\n');
        return sb.toString();
    }

    /**
     * Handle the adopt request data.
     *
     * @return true if adoption request handled successfully.
     */
    private boolean handleAdoptionRequest(VirtualEap virtualEap, Map<String, Map<String, Object>> requestDataMap) {
        Map<String, Object> bodyMap = requestDataMap.get("body");
        if (bodyMap != null && !bodyMap.isEmpty()) {
            // The data from EAP controller.
            String auth = (String) bodyMap.get("auth");
            String sign = (String) bodyMap.get("sign");
            return verifyAuth(virtualEap, auth, sign);
        } else {
            return false;
        }
    }

    /**
     * Verify the auth and sign send by server, using the RSA public key from application.properties.
     *
     * @param auth The encrypted username and password.
     * @param sign The signature of auth, using SHA1 hash.
     * @return true if verify successfully.
     */
    protected boolean verifyAuth(VirtualEap virtualEap, String auth, String sign) {
        if (publicKeyStr == null || "".equals(publicKeyStr)) {
            logger.error("Can't obtain property: 'public.key' from resource: 'application.properties'");
            return false;
        } else {
            String _auth = auth.trim().replaceAll("\n", "").replaceAll(" ", "")
                    .replaceAll("\\n", "").replaceAll("\r", "").replaceAll("\\r", "");
            String _sign = sign.trim().replaceAll("\n", "").replaceAll(" ", "")
                    .replaceAll("\\n", "").replaceAll("\r", "").replaceAll("\\r", "");
            boolean v = RSAHelper.verifySHA1WithPublicKey(_auth, _sign, publicKeyStr);
            if (!v) {
                return false;
            } else {
                String res = new String(RSAHelper.decryptWithPublicKey(_auth, publicKeyStr), Charset.forName("UTF-8"));
                String[] data = res.split("\u0000");
                if (data.length != 2) {
                    return false;
                } else {
                    String username = data[0];
                    String passwordMd5 = data[1];
                    if (username.equals(virtualEap.getUserName()) && passwordMd5.equals(virtualEap.getPasswordMD5())) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    private boolean isRC4Used(VirtualEap eap) {
        VirtualEap.State state = eap.getCurrentState();
        return !(state == VirtualEap.State.INIT ||
                state == VirtualEap.State.PENDING ||
                state == VirtualEap.State.CONNECTING);
    }
}
