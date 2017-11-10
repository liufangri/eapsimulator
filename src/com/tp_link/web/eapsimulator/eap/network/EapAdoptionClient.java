/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-28
 */

package com.tp_link.web.eapsimulator.eap.network;

import com.tp_link.web.eapsimulator.eap.VirtualEap;
import com.tp_link.web.eapsimulator.eap.network.log.NetLog;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataBody;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataHeader;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataType;
import com.tp_link.web.eapsimulator.eap.network.protocol.Packet;
import com.tp_link.web.eapsimulator.tools.GsonHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;

/**
 * The EAP Adoption client.
 */
public class EapAdoptionClient extends EapClient {

    public static final String DEFAULT_PRE_ADOPT_DEST_ADDRESS = "127.0.0.1";

    private EventLoopGroup workGroup;
    private Bootstrap bootstrap;
    private EapAdoptionHandler adoptionHandler;
    private NetLog netLog;

    private Map<Integer, VirtualEap> portMap = new HashMap<>();
    private Map<String, Channel> channelMap = new HashMap<>();

    private static final Log logger = LogFactory.getLog(EapAdoptionClient.class);

    /**
     * The Adoption client creator.
     *
     * @param destAddress The destination ip address.
     * @param destPort    The destination port.
     * @return An new instance of EapAdoptionClient.
     */
    public static EapAdoptionClient getInstance(String destAddress, int destPort) {
        return new EapAdoptionClient(destAddress, destPort);
    }

    private EapAdoptionClient(String destAddress, int destPort) {
        super(destAddress, destPort);
    }

    public void setAdoptionHandler(EapAdoptionHandler adoptionHandler) {
        this.adoptionHandler = adoptionHandler;
    }

    @Override
    public void run() throws InterruptedException {
        this.bootstrap();
    }

    private void bootstrap() {
        bootstrap = new Bootstrap();
        workGroup = new NioEventLoopGroup();

        bootstrap.group(workGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline().addLast(adoptionHandler);
                    }
                });
    }

    @Override
    public boolean stop() throws InterruptedException {

        //TODO: close all channels.

        if (workGroup != null) {
            workGroup.shutdownGracefully();
        }

        if (adoptionHandler != null) {
            adoptionHandler.setContext(null);
            adoptionHandler = null;
        }
        return true;
    }

    @Override
    public void removeEap(VirtualEap eap) {
        portMap.remove(eap.getAdoptPort());
        try {
            Channel channel = channelMap.get(eap.getMac());
            if (channel != null) {
                channel.close().sync();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted Stopping eap adopt channel!");
        } finally {
            channelMap.remove(eap.getMac());
        }
        super.removeEap(eap);
    }

    public VirtualEap getEapByPort(int port) {
        return portMap.get(port);
    }

    public Channel connect(VirtualEap eap, String destAddress, int destPort) throws InterruptedException {
        Channel channel;
        channel = channelMap.get(eap.getMac());

        if (channel != null && channel.isActive()) {
            if (((InetSocketAddress) channel.localAddress()).getPort() == eap.getAdoptPort()) {
                return channel;
            } else {
                channel.close().sync();
            }
        }
        try {
            channel = bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
                    .localAddress(eap.getAdoptPort())
                    .connect(destAddress, destPort).sync().channel();
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof InterruptedException) {
                // Interrupted Unexpected
                throw e;
            } else if (e instanceof RejectedExecutionException) {
                // event executor terminated!
                throw new InterruptedException("event executor terminated.");
            } else {
                logger.error(e.getMessage());
                if (e.getCause() instanceof BindException || e.getCause() instanceof ConnectTimeoutException) {
                    // Change port and try again.
                    Thread.sleep(100);
                    int port = getPort();
                    eap.setManagePort(port);
                    bootstrap.localAddress(port);
                    channel = connect(eap, destAddress, destPort);
                } else {
                    e.printStackTrace();
                    channel = null;
                }
            }
        }
        if (channel == null) {
            logger.error("EAP - " + eap.getMac() + " Init channel error.");
        } else {
            channelMap.put(eap.getMac(), channel);
            portMap.put(eap.getAdoptPort(), eap);
        }
        return channel;
    }

    /**
     * Send pre-connect information message to server.
     */
    public void sendPreConnectInformation(VirtualEap virtualEap) {
        Channel channel = channelMap.get(virtualEap.getMac());
        if (channel == null) {
            throw new RuntimeException("Channel is null! EAP: " + virtualEap.getMac());
        }
        DataHeader header = new DataHeader(
                DataHeader.PROTOCOL_VERSION, virtualEap.getMac(), DataType.PRE_CONNECT_INFO, 0);
        DataBody body = new DataBody();
        Packet packet = new Packet(header, body);
        String data = GsonHelper.getGson().toJson(packet.getData());
        byte[] jsonBytes = data.getBytes(Charset.forName("UTF-8"));
        int length = jsonBytes.length;
        ByteBuf byteBuf = channel.alloc().buffer(length);
        byteBuf.writeInt(length).writeBytes(jsonBytes);
        channel.writeAndFlush(byteBuf);
        netLog.log(virtualEap, data, "SEND", DataType.PRE_CONNECT_INFO,
                (InetSocketAddress) channel.localAddress(), (InetSocketAddress) channel.remoteAddress());
    }

    public EapAdoptionClient setNetLog(NetLog netLog) {
        this.netLog = netLog;
        return this;
    }

}
