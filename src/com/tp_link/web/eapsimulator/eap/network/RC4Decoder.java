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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class RC4Decoder extends ByteToMessageDecoder {
    private static final Log logger = LogFactory.getLog(RC4Decoder.class);
    private byte[] key;
    private boolean data = false;
    private int len = 4;

    public RC4Decoder(byte[] key) {
        this.key = key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() >= len) {
            byte[] inBytes;
            try {
                inBytes = new byte[len];
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            in.readBytes(inBytes, 0, len);
            RC4Helper.RC4(inBytes, inBytes, key);
            if (data) {
                out.add(inBytes);
                len = 4;
                data = false;
            } else {
                len = TypeConvert.bytesToInt(inBytes, 0);
                if (len <= 0) {
                    logger.error("Decode length error.");
                }
                data = true;
            }
        }
    }
}
