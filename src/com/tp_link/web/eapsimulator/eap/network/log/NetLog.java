/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-10-19
 */

package com.tp_link.web.eapsimulator.eap.network.log;

import com.tp_link.web.eapsimulator.eap.VirtualEap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;

public interface NetLog<T> {

    /**
     * Log the data, Used ONLY for RECEIVE UDP packet.
     *
     * @param eap
     * @param comment
     * @param action
     * @param type
     * @param packet
     */
    default NetLog<T> log(VirtualEap eap, String comment, String action, int type, DatagramPacket packet) {
        return log(eap, comment, action, type, packet.sender(), packet.recipient());
    }


    /**
     * Log the data, Used for SEND or RECEIVE.
     *
     * @param eap
     * @param comment
     * @param action
     * @param type
     * @param ctx
     */
    default NetLog<T> log(VirtualEap eap, String comment, String action, int type, ChannelHandlerContext ctx) {
        return log(eap, comment, action, type, ctx.channel());
    }

    /**
     * Log the data, Used for SEND or RECEIVE.
     *
     * @param eap
     * @param comment
     * @param action
     * @param type
     * @param channel
     */
    default NetLog<T> log(VirtualEap eap, String comment, String action, int type, Channel channel) {
        if ("SEND".equals(action)) {
            return log(eap, comment, action, type, (InetSocketAddress) channel.localAddress(),
                    (InetSocketAddress) channel.remoteAddress());
        } else {
            return log(eap, comment, action, type, (InetSocketAddress) channel.remoteAddress(),
                    (InetSocketAddress) channel.localAddress());
        }
    }

    /**
     * Log the data, Used for SEND or RECEIVE.
     *
     * @param eap
     * @param comment
     * @param action
     * @param type
     * @param source
     * @param dest
     */
    default NetLog<T> log(VirtualEap eap, String comment, String action, int type, InetSocketAddress source, InetSocketAddress dest) {
        return log(eap, comment, action, type, source.getAddress().getHostAddress(), source.getPort(),
                dest.getAddress().getHostAddress(), dest.getPort());
    }

    /**
     * Log the data, Used for SEND or RECEIVE.
     *
     * @param eap
     * @param comment
     * @param action
     * @param type
     * @param sourceAddress
     * @param sourcePort
     * @param destAddress
     * @param destPort
     */
    NetLog<T> log(VirtualEap eap, String comment, String action, int type, String sourceAddress,
                  int sourcePort, String destAddress, int destPort);

    NetLog<T> log(T t, String comment, String action);

}
