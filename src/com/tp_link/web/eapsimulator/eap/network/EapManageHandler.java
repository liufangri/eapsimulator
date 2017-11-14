/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-10-16
 */

package com.tp_link.web.eapsimulator.eap.network;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.tp_link.web.eapsimulator.eap.VirtualEap;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataBody;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataHeader;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataType;
import com.tp_link.web.eapsimulator.eap.network.protocol.Packet;
import com.tp_link.web.eapsimulator.tools.GsonHelper;
import com.tp_link.web.eapsimulator.tools.RC4Helper;
import com.tp_link.web.eapsimulator.tools.RSAHelper;
import com.tp_link.web.eapsimulator.tools.TypeConvert;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

@ChannelHandler.Sharable
public class EapManageHandler extends ChannelInboundHandlerAdapter {

    private static final Log logger = LogFactory.getLog(EapManageHandler.class);
    private EapNetContext context;

    //TODO: release after stop eap.
    private Map<Integer, MsgControl> controlMap = new HashMap<>();

    public void setNetContext(EapNetContext netContext) {
        this.context = netContext;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        int port = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
        VirtualEap virtualEap = context.getManageClient().getEapByPort(port);
        if (virtualEap == null) {
            // No virtualEap has this port.
            return;
        }

        MsgControl msgControl = controlMap.get(port);
        if (msgControl == null) {
            // The first time using msgControl.
            logger.debug("Init msgControl for EAP-" + virtualEap.getMac());
            msgControl = new MsgControl();
            msgControl.initFirst();
            controlMap.put(port, msgControl);
        }

//        String message = new String(msg);
//        processData(virtualEap, ctx, message);

        ByteBuf byteBuf = (ByteBuf) msg;
        int ret = msgControl.isReadyToHandle(virtualEap, byteBuf);
        while (ret == MsgControl.HAVE_MORE) {
            String message = new String(msgControl.dataBytes);
            processData(virtualEap, ctx, message);
            msgControl.dataBytes = null;
            ret = msgControl.isReadyToHandle(virtualEap, byteBuf);
        }
        if (ret == MsgControl.READY) {
            String message = new String(msgControl.dataBytes);
            msgControl.dataBytes = null;
            processData(virtualEap, ctx, message);
        } else if (ret == MsgControl.NOT_READY) {
            logger.debug("EAP - " + virtualEap.getMac() + " received but still more, current: " +
                    msgControl.offset + " expect: " + msgControl.length);
        } else {
            logger.error("EAP - " + virtualEap.getMac() + " received wrong message!");
        }
    }

    public void removeEapMsgCtr(Integer port) {
        controlMap.remove(port);
    }

    private boolean processData(VirtualEap virtualEap, ChannelHandlerContext ctx, String data) {
        logger.debug("EAP - " + virtualEap.getMac() + " Received: " + data);
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
            if (type != DataType.INFORM_RESPONSE) {
                context.getNetLog().log(virtualEap, data, "RECEIVE", type, ctx);
            }
            // Handle This message.

            if (type == DataType.INFORM_RESPONSE) {
                // TODO: what to do ?
            } else if (type == DataType.SET_REQUEST) {
                if (handleSetRequest(requestDataMap, virtualEap, ctx)) {
                    synchronized (virtualEap.lock) {
                        virtualEap.lock.notify();
                    }
                } else {
                    logger.error("Received wrong setting request! EAP - " + virtualEap.getMac());
                }
            } else if (type == DataType.FORGET_REQUEST || type == DataType.FORGET_REQUEST_NO_RESET) {
                VirtualEap.State state = virtualEap.getCurrentState();
                if (state == VirtualEap.State.CONNECTED || state == VirtualEap.State.CONFIGURING) {
                    // Stop send data.
                    context.doForgetOperation(virtualEap);
                    virtualEap.setCurrentState(VirtualEap.State.INIT);
                    synchronized (virtualEap.lock) {
                        virtualEap.lock.notify();
                    }
                }
            }
            return true;
        } catch (JsonParseException e) {
            logger.debug(e.getMessage());
            return false;
        }
    }

    private boolean handleSetRequest(Map<String, Map<String, Object>> requestDataMap,
                                     VirtualEap virtualEap, ChannelHandlerContext ctx) {

        Map<String, Object> body = requestDataMap.get("body");
        Map<String, String> userAccount = (Map<String, String>) body.get("userAccount");
        if (userAccount != null) {
            virtualEap.setUserName(userAccount.get("newUsername"));
            virtualEap.setPasswordMD5(userAccount.get("newPassword"));
        }
        virtualEap.setCurrentState(VirtualEap.State.CONFIGURING);
        //TODO: handle config.
        // Get sequenceId
        int sequenceId;
        try {
            Object o = body.get("sequenceId");
            if (o instanceof Double) {
                sequenceId = ((Double) o).intValue();
            } else if (o instanceof String) {
                sequenceId = Integer.parseInt((String) o);
            } else if (o instanceof Integer) {
                sequenceId = (Integer) o;
            } else {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        sendConfigResponse(ctx, virtualEap, sequenceId);
        virtualEap.setCurrentState(VirtualEap.State.CONNECTED);

        return true;
    }

    private void sendConfigResponse(ChannelHandlerContext ctx, VirtualEap virtualEap, int sequenceId) {
        DataHeader header = new DataHeader();
        header.setType(DataType.SET_RESPONSE);
        header.setMac(virtualEap.getMac());
        DataBody body = new DataBody();
        body.put("sequenceId", sequenceId);
        body.put("errcode", 0);
        Packet packet = new Packet(header, body);

        String keyBase64 = virtualEap.getRc4Key();
        if (keyBase64 != null) {
            byte[] key = RSAHelper.decryptBASE64(keyBase64);
            String json = GsonHelper.getGson().toJson(packet.getData());
            int len = json.getBytes().length;
            try {
                byte[] byteLen = new byte[4];
                TypeConvert.intToBytes(len, byteLen, 0);
                byteLen = RC4Helper.RC4(byteLen, byteLen, key);
                ctx.writeAndFlush(ctx.alloc().buffer(4).writeBytes(byteLen, 0, 4)).sync();

                byte[] byteJson = RC4Helper.RC4(json.getBytes(), key);
                ctx.writeAndFlush(ctx.alloc().buffer(len).writeBytes(byteJson, 0, len)).sync();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }

            context.getNetLog().log(virtualEap, json, "SEND", DataType.SET_RESPONSE, ctx);
        }
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

    private class MsgControl {
        static final int READY = 1;
        static final int NOT_READY = 0;
        static final int HAVE_MORE = 2;
        static final int LENGTH_ERROR = -1;
        static final int STATE_ERROR = -2;
        static final int KEY_IS_NULL = -3;
        // The max length of data can handle.
        static final int MAX_LEN = 64 * 1024 * 1024;

        // Next part of packet is data.
        boolean nextData = false;

        boolean nextLength = false;

        // The length of data.
        int length = 4;

        int offset = 0;

        byte[] dataBytes;

        public void initFirst() {
            nextData = false;
            length = 4;
            dataBytes = null;
            offset = 0;
        }

        public int isReadyToHandle(VirtualEap virtualEap, ByteBuf byteBuf) {
            String keyStr = virtualEap.getRc4Key();
            if (keyStr == null) {
                return STATE_ERROR;
            } else {
                byte[] key = RSAHelper.decryptBASE64(keyStr);
                if (!nextData) {
                    if (!nextLength) {
                        if (byteBuf.readableBytes() > 4) {
                            byte[] lenBytes = new byte[4];
                            byteBuf.readBytes(lenBytes, 0, 4);
                            RC4Helper.RC4(lenBytes, lenBytes, key);
                            int len = TypeConvert.bytesToInt(lenBytes, 0);
                            if (len > 0 && len <= MAX_LEN) {
                                int cap = byteBuf.readableBytes();
                                dataBytes = new byte[len];

                                if (len > cap) {
                                    byteBuf.readSlice(cap).readBytes(dataBytes, 0, cap);
                                    nextData = true;
                                    length = len;
                                    offset = cap;
                                    return NOT_READY;
                                } else if (len == byteBuf.readableBytes()) {
                                    byteBuf.readBytes(dataBytes, 0, byteBuf.readableBytes());
                                    RC4Helper.RC4(dataBytes, dataBytes, key);
                                    nextData = false;
                                    length = 4;
                                    offset = 0;
                                    return READY;
                                } else {
                                    byteBuf.readBytes(dataBytes, 0, len);
                                    RC4Helper.RC4(dataBytes, dataBytes, key);
                                    nextData = false;
                                    length = 4;
                                    offset = 0;
                                    return HAVE_MORE;
                                }
                            } else {
                                logger.debug("EAP - " + virtualEap.getMac() + " received error length: " + len);
                                return LENGTH_ERROR;
                            }
                        } else if (byteBuf.readableBytes() == 4) {
                            byte[] lenBytes = new byte[4];

                            byteBuf.readBytes(lenBytes, 0, 4);
                            RC4Helper.RC4(lenBytes, lenBytes, key);
                            int len = TypeConvert.bytesToInt(lenBytes, 0);
                            if (0 < len && len <= MAX_LEN) {
                                dataBytes = new byte[len];
                            }
                            nextData = true;
                            length = len;
                            offset = 0;
                            return NOT_READY;
                        } else if (byteBuf.readableBytes() > 0) {
                            // Readable bytes less than 4
                            dataBytes = new byte[4];
                            int cap = byteBuf.readableBytes();
                            byteBuf.readBytes(dataBytes, 0, cap);
                            nextLength = true;
                            length = 4;
                            offset = cap;
                            return NOT_READY;
                        } else {
                            return READY;
                        }
                    } else {
                        // Read length bytes.
                        int cap = byteBuf.readableBytes();
                        if (length < cap + offset) {
                            byteBuf.readBytes(dataBytes, offset, length - offset);
                            RC4Helper.RC4(dataBytes, dataBytes, key);
                            int len = TypeConvert.bytesToInt(dataBytes, 0);
                            nextLength = false;
                            nextData = true;
                            length = len;
                            if (0 < len && len <= MAX_LEN) {
                                dataBytes = new byte[len];
                            }
                            offset = 0;
                            return isReadyToHandle(virtualEap, byteBuf);
                        } else if (length == cap + offset) {
                            byteBuf.readBytes(dataBytes, offset, cap);
                            RC4Helper.RC4(dataBytes, dataBytes, key);
                            int len = TypeConvert.bytesToInt(dataBytes, 0);
                            nextLength = false;
                            nextData = true;
                            length = len;
                            offset = 0;
                            if (0 < len && len <= MAX_LEN) {
                                dataBytes = new byte[len];
                            }
                            return NOT_READY;
                        } else if (cap > 0) {
                            byteBuf.readBytes(dataBytes, offset, cap);
                            offset += cap;
                            return NOT_READY;
                        } else {
                            return READY;
                        }
                    }
                } else {
                    int cap = byteBuf.readableBytes();
                    if (length > cap + offset) {
                        byteBuf.readBytes(dataBytes, offset, cap);
                        offset += cap;
                        return NOT_READY;
                    } else if (length == cap + offset) {
                        byteBuf.readBytes(dataBytes, offset, cap);
                        RC4Helper.RC4(dataBytes, dataBytes, key);
                        nextData = false;
                        length = 0;
                        offset = 0;
                        return READY;
                    } else {
                        byteBuf.readBytes(dataBytes, offset, length - offset);
                        RC4Helper.RC4(dataBytes, dataBytes, key);
                        nextData = false;
                        length = 4;
                        offset = 0;
                        return HAVE_MORE;
                    }
                }
            }
        }
    }
}
