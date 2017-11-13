/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-11-13
 */

package com.tp_link.web.eapsimulator.eap.network;

import com.tp_link.web.eapsimulator.tools.RC4Helper;
import com.tp_link.web.eapsimulator.tools.TypeConvert;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class RC4Decoder extends ByteToMessageDecoder {
    byte[] key;
    boolean data = false;
    int len = 4;

    public void setKey(byte[] key) {
        this.key = key;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() >= len) {
            byte[] inBytes = new byte[len];
            in.readBytes(inBytes, 0, len);
            RC4Helper.RC4(inBytes, inBytes, key);
            if (data) {
                out.add(inBytes);
                data = false;
                len = 4;
            } else {
                len = TypeConvert.bytesToInt(inBytes, 0);
                data = true;
            }
        }

    }
}
