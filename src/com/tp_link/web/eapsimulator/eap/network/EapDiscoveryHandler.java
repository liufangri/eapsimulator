/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-27
 */

package com.tp_link.web.eapsimulator.eap.network;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.tp_link.web.eapsimulator.eap.VirtualEap;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataType;
import com.tp_link.web.eapsimulator.tools.GsonHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Handle the Pre-Adopt request from EAP controller.
 */
public class EapDiscoveryHandler extends SimpleChannelInboundHandler<DatagramPacket> {


    private static final Log logger = LogFactory.getLog(EapDiscoveryHandler.class);

    private EapNetContext context;

    // The data within pre-adaption request packet.
    private Map<String, Map<String, String>> dataMap;

    public void setContext(EapNetContext context) {
        this.context = context;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        // Handle pre-adoption UDP
        ByteBuf byteBuf = msg.content();
        int length = byteBuf.readInt(); // The data length
        String data = byteBuf.toString(Charset.forName("UTF-8")); // The data content
        logger.debug("Received: " + data);

        //  Handle data.
        handlePreAdoptionPacket(msg, length, data);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        logger.error(throwable);
    }

    /**
     * Handle the data of pre-adopt request. pause the discovery broadcast.
     *
     * @param msg
     * @param length The length of data.
     * @param data   The data in udp packet
     */
    protected void handlePreAdoptionPacket(DatagramPacket msg, int length, String data) {
        int dataLen = data.getBytes(Charset.forName("UTF-8")).length;
        try {
            if (length == dataLen) {
                Type typeOfT = new TypeToken<Map<String, Map<String, String>>>() {
                }.getType();
                dataMap = GsonHelper.getGson().fromJson(data, typeOfT);

                if (dataMap != null && !dataMap.isEmpty()) {
                    Map<String, String> headerMap = dataMap.get("header");

                    if (headerMap != null && !headerMap.isEmpty()) {
                        String typeStr = headerMap.get("type");
                        int type = Integer.valueOf(typeStr);

                        if (type == DataType.PRE_ADOPT_REQUEST) {

                            Map<String, String> bodyMap = dataMap.get("body");
                            String serverIP = bodyMap.get("ip"); //server IP.
                            String mac = bodyMap.get("mac");// EAP mac

                            if (serverIP != null && !"".equals(serverIP)) {
                                context.setServerIP(serverIP);
                                if (mac != null && !"".equals(mac)) {
                                    VirtualEap virtualEap = context.getVirtualEapByMac(mac);
                                    context.getNetLog().log(virtualEap, data, "RECEIVE", type, msg);
                                    virtualEap.setCurrentState(VirtualEap.State.CONNECTING);
                                    synchronized (virtualEap.lock) {
                                        virtualEap.lock.notify();
                                    }
                                }

                                return;
                            }
                        }
                    }
                }
            }else{
                logger.debug("Received complicate UDP packet.");
            }
        } catch (JsonParseException e1) {
            return;

        } catch (NumberFormatException e2) {
            // Do nothing.
        }
        dataMap = null;
    }

    public Map<String, Map<String, String>> getDataMap() {
        return dataMap;
    }


}
