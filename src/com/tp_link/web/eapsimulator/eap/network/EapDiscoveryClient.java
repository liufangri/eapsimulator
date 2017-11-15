/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-25
 */

package com.tp_link.web.eapsimulator.eap.network;

import com.tp_link.web.eapsimulator.eap.VirtualEap;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataBody;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataHeader;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataType;
import com.tp_link.web.eapsimulator.eap.network.protocol.Packet;
import com.tp_link.web.eapsimulator.tools.GsonHelper;
import com.tp_link.web.eapsimulator.eap.network.log.NetLog;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.internal.SocketUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

public class EapDiscoveryClient extends EapClient {

    private static final Log logger = LogFactory.getLog(EapDiscoveryClient.class);
    private static final long DISCOVERY_SLEEP_TIME = 5000;

    private Bootstrap bootstrap;
    private EventLoopGroup workGroup;
    private EapDiscoveryHandler discoveryHandler;
    private Channel channel;
    private NetLog netLog;
    private int port;


    private Thread broadcastThread;
    private BroadcastRunnable broadcastRunnable = new BroadcastRunnable();

    private boolean stopped = false;
    private boolean terminated = false;

    public static final String DEFAULT_DEST_ADDRESS = "255.255.255.255";
    public static final int DEFAULT_DEST_PORT = 29810;

    /**
     * Create an EAP discovery client instance and return it.
     *
     * @param destAddress The destination IP address.
     * @param destPort    The destination port.
     * @return A new EAP discovery client object.
     */
    public static EapDiscoveryClient getInstance(String destAddress, int destPort) {
        return new EapDiscoveryClient(destAddress, destPort);
    }

    private EapDiscoveryClient(String destAddress, int port) {
        super(destAddress, port);
    }

    public void setDiscoveryHandler(EapDiscoveryHandler discoveryHandler) {
        this.discoveryHandler = discoveryHandler;
    }

    @Override
    /**
     * This will start broadcast thread.
     */
    public void run() throws InterruptedException {
        this.bootstrap();
        port = getPort();
        try {
            // Be careful with BindException.
            channel = connect(bootstrap, port);
            startBroadcast();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    protected void bootstrap() {
        bootstrap = new Bootstrap();
        workGroup = new NioEventLoopGroup();

        bootstrap.group(workGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(discoveryHandler);
    }

    protected static synchronized Channel connect(Bootstrap bootstrap, int port) throws Exception {
        Channel channel = null;
        try {
            channel = bootstrap.bind(port).sync().channel();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
        return channel;
    }

    /**
     * Broadcast Discovery datagram to port 29810.
     */
    public void broadcastDiscoveryDatagram(DataHeader header, DataBody body, Packet packet,
                                           VirtualEap virtualEap, boolean writeLog) {

        refreshHeadAndBody(header, body, virtualEap);
        String json = GsonHelper.getGson().toJson(packet.getData());
        DatagramPacket datagram = sendDiscovery(json);
        if (writeLog) {
            netLog.log(virtualEap, json, "SEND", DataType.DISCOVERY,
                    (InetSocketAddress) channel.localAddress(), datagram.recipient());
        }
        json = null;
        datagram = null;
    }

    private DatagramPacket sendDiscovery(String json) {
        ByteBuf byteBuf = channel.alloc().buffer();
        byte[] bytes = json.getBytes(Charset.forName("UTF-8"));

        byteBuf.writeInt(bytes.length);
        byteBuf.writeBytes(bytes);

        // Send UDP packet
        DatagramPacket datagramPacket = new DatagramPacket(byteBuf, SocketUtils.socketAddress(destAddress, destPort));
        channel.writeAndFlush(datagramPacket);
        return datagramPacket;
    }

    public void startBroadcast() {
        if (broadcastThread == null || !broadcastThread.isAlive() || stopped) {
            broadcastThread = new Thread(broadcastRunnable);
            broadcastThread.setName("EAP-Broadcast");
            broadcastThread.start();
        }
        stopped = false;
    }

    public void stopBroadcast() {
        if (broadcastThread != null) {
            stopped = true;
            broadcastThread.interrupt();
        }
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            logger.debug(e.getMessage());
        }
        if (broadcastThread.isAlive()) {
            stopBroadcast();
        }
    }

    public synchronized void refreshHeadAndBody(DataHeader header, DataBody body, VirtualEap virtualEap) {
        header.setMac(virtualEap.getMac());
        header.setVersion(DataHeader.PROTOCOL_VERSION);
        header.setType(DataType.DISCOVERY);
        header.setError(0);

        Map<String, Object> deviceInfo = new LinkedHashMap<String, Object>();
        deviceInfo.put("model", virtualEap.getModel());
        deviceInfo.put("name", virtualEap.getName());
        deviceInfo.put("firmwareVersion", virtualEap.getFirmwareVersion());
        deviceInfo.put("upTime", virtualEap.getUpTime());
//        deviceInfo.put("cpuUti", 70);
//        deviceInfo.put("memUti", 80);
        deviceInfo.put("ip", virtualEap.getIp());
        deviceInfo.put("modelVersion", virtualEap.getModelVersion());
        deviceInfo.put("hardwareVersion", virtualEap.getHardwareVersion());
//        deviceInfo.put("mask", virtualEap.getMask());

        Map<String, Object> deviceMisc = new LinkedHashMap<String, Object>();
        deviceMisc.put("support_5g", virtualEap.getSupport_5g());
        deviceMisc.put("support_11ac", virtualEap.getSupport_11ac());
        deviceMisc.put("support_lag", virtualEap.getSupport_lag());
        deviceMisc.put("customizeRegion", virtualEap.getCustomizeRegion());
        deviceMisc.put("minPower2G", virtualEap.getMinPower2G());
        deviceMisc.put("maxPower2G", virtualEap.getMaxPower2G());

        body.put("deviceInfo", deviceInfo);
        body.put("deviceMisc", deviceMisc);
    }

    @Override
    public boolean stop() throws InterruptedException {
        if (!terminated) {
            //TODO: Stop client properly.
            if (broadcastThread != null) {
                broadcastThread.interrupt();
            }
            stopped = true;
            if (workGroup != null) {
                workGroup.shutdownGracefully();
            }
            if (channel != null) {
                channel.closeFuture().sync(); // Block to stop
            }
            terminated = true;

            if (discoveryHandler != null) {
                discoveryHandler.setContext(null);
                discoveryHandler = null;
            }
            return true;
        } else {
            return false;
        }
    }

    public EapDiscoveryClient setNetLog(NetLog netLog) {
        this.netLog = netLog;
        return this;
    }

    @Override
    public void addEap(VirtualEap eap) {
        super.addEap(eap);
        DataHeader header = new DataHeader();
        DataBody body = new DataBody();
        Packet packet = new Packet(header, body);
        broadcastDiscoveryDatagram(header, body, packet, eap, true);
    }

    class BroadcastRunnable implements Runnable {
        DataHeader header = new DataHeader();
        DataBody body = new DataBody();
        Packet packet = new Packet(header, body);

        @Override
        public void run() {
            while (!stopped) {
                for (Map.Entry<String, VirtualEap> entry : eapMap.entrySet()) {
                    VirtualEap virtualEap = entry.getValue();
                    if (virtualEap != null) {
                        broadcastDiscoveryDatagram(header, body, packet, virtualEap, false);
                        if (stopped) {
                            break;
                        }
                    } else {
                        logger.debug("Broadcast EAP is null.");
                    }
                }
                try {
                    Thread.sleep(DISCOVERY_SLEEP_TIME);
                } catch (InterruptedException e) {
                    logger.debug("Broadcast thread interrupted!");
                    break;
                }
                if (Thread.interrupted()) {
                    logger.debug("Broadcast thread interrupted!");
                    break;
                }
            }
            logger.info("Broadcast thread stopped.");
        }
    }
}
