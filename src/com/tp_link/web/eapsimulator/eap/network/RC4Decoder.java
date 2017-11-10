/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-11-07
 */

package com.tp_link.web.eapsimulator.eap.network;

import com.tp_link.web.eapsimulator.eap.VirtualEap;
import com.tp_link.web.eapsimulator.tools.RC4Helper;
import com.tp_link.web.eapsimulator.tools.RSAHelper;
import com.tp_link.web.eapsimulator.tools.TypeConvert;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This Decoder process input bytes encrypted by RC4 and returns String.
 */
public class RC4Decoder extends ByteToMessageDecoder {
    private EapManageClient manageClient;

    private Map<Integer, MsgControl> expLengthMap = new HashMap<>();

    public void setManageClient(EapManageClient manageClient) {
        this.manageClient = manageClient;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int port = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
        MsgControl msgControl = expLengthMap.get(port);
        if (msgControl == null) {
            msgControl = new MsgControl();
            msgControl.nextData = false;
            msgControl.length = 4;
            msgControl.handle = true;
            expLengthMap.put(port, msgControl);
        }
        if (in.readableBytes() >= msgControl.length) {
            VirtualEap virtualEap = manageClient.getEapByPort(port);

            byte[] dataBytes = new byte[msgControl.length];
            in.readBytes(dataBytes, 0, msgControl.length);

            if (virtualEap != null) {
                byte[] key = RSAHelper.decryptBASE64(virtualEap.getRc4Key());
                RC4Helper.RC4(dataBytes, dataBytes, key);
                if (!msgControl.nextData) {
                    // This is a length;
                    int len = TypeConvert.bytesToInt(dataBytes, 0);
                    msgControl.handle = len < MsgControl.MAX_LEN;
                    msgControl.length = len;
                    msgControl.nextData = true;
                } else {
                    if (msgControl.handle) {
                        out.add(new String(dataBytes));
                    }
                    msgControl.handle = true;
                    msgControl.nextData = false;
                    msgControl.length = 4;
                }
            }
        }
    }

   private class MsgControl {
        // The max length of data can handle.
        static final int MAX_LEN = 2 * 1024 * 1024;

        // Next part of packet is data.
        boolean nextData = false;

        // The length of data.
        int length = 4;

        // Handle data or not.
        boolean handle = true;
    }
}
