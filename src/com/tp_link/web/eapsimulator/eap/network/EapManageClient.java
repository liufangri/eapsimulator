/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-10-16
 */

package com.tp_link.web.eapsimulator.eap.network;

import com.tp_link.web.eapsimulator.eap.VirtualEap;
import com.tp_link.web.eapsimulator.eap.network.log.NetLog;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataBody;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataHeader;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataType;
import com.tp_link.web.eapsimulator.eap.network.protocol.Packet;
import com.tp_link.web.eapsimulator.tools.GsonHelper;
import com.tp_link.web.eapsimulator.tools.RC4Helper;
import com.tp_link.web.eapsimulator.tools.RSAHelper;
import com.tp_link.web.eapsimulator.tools.TypeConvert;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;

public class EapManageClient extends EapClient {
    public static final String DEFAULT_MANAGE_ADDRESS = "127.0.0.1";
    public static final long INFORM_SLEEP_TIME = 10000;
    public static final int CAPABILITY = 1024;

    private static final Log logger = LogFactory.getLog(EapManageClient.class);
    private Bootstrap bootstrap;
    private EventLoopGroup workGroup;
    private EapManageHandler manageHandler;
    private NetLog netLog;

    private Map<String, Channel> channelMap = new HashMap<>();
    private Map<Integer, VirtualEap> portMap = new HashMap<>();

    private boolean stopped = false;
    private Thread sendInformRequestThread;

    private SendInformRequestRunnable informRequestRunnable = new SendInformRequestRunnable();

    private EapManageClient(String destAddress, int destPort) {
        super(destAddress, destPort);
    }

    public static EapManageClient getInstance(String destAddress, int destPort) {
        return new EapManageClient(destAddress, destPort);
    }


    public void setManageHandler(EapManageHandler manageHandler) {
        this.manageHandler = manageHandler;
    }

    @Override
    public void run() throws InterruptedException {
        bootstrap();
        startSendInformRequest();
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
        manageHandler.removeEapMsgCtr(eap.getManagePort());
        super.removeEap(eap);
    }

    public void bootstrap() {
        workGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();

        bootstrap.group(workGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(manageHandler);
                    }
                });
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
                    .localAddress(eap.getManagePort())
                    .connect(destAddress, destPort).sync().channel();
        } catch (Exception e) {
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
            portMap.put(eap.getManagePort(), eap);
        }
        return channel;
    }

    @Override
    public boolean stop() throws InterruptedException {
        //TODO: stop inform thread and close channels.
        if (workGroup != null) {
            workGroup.shutdownGracefully();
        }
        stopSendInformRequestThread();
        return true;
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

    public void startSendInformRequest() {
        if (sendInformRequestThread == null || !sendInformRequestThread.isAlive() || stopped) {
            sendInformRequestThread = new Thread(informRequestRunnable);
            sendInformRequestThread.setName("EAP-Inform");
            sendInformRequestThread.start();
        }
        stopped = false;
    }

    public void stopSendInformRequestThread() {
        if (sendInformRequestThread != null) {
            stopped = true;
            sendInformRequestThread.interrupt();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.debug(e.getMessage());
            }
            if (sendInformRequestThread.isAlive()) {
                // Interrupt failed, Try again.
                stopSendInformRequestThread();
            }
            sendInformRequestThread = null;
        }
    }

    public void stopSendInform(VirtualEap virtualEap) {
        removeEap(virtualEap);
    }

    public void sendInformRequest(VirtualEap virtualEap, boolean writeLog) {
        DataHeader header = new DataHeader();
        DataBody body = new DataBody();
        Packet packet = new Packet(header, body);
        Channel channel = channelMap.get(virtualEap.getMac());
        if (channel == null) {
            logger.error("EAP - " + virtualEap.getMac() + " channel is null");
            return;
        }
        refreshInformRequest(virtualEap, header, body, packet);
        String json = GsonHelper.getGson().toJson(packet.getData());

        // The RC4 key
        byte[] key = RSAHelper.decryptBASE64(virtualEap.getRc4Key());

        // The data to send.
        byte[] bytes = RC4Helper.RC4(json.getBytes(), key);

        int length = bytes.length;
        byte[] lengthBytes = new byte[4];
        TypeConvert.intToBytes(length, lengthBytes, 0);


        if (channel.isWritable()) {
            if (length > CAPABILITY) { // Divide packet.
                // Write length.
                lengthBytes = RC4Helper.RC4(lengthBytes, key);
                channel.writeAndFlush(channel.alloc().buffer(4).writeBytes(lengthBytes, 0, 4));

                // Write divided packets.
                int index = 0;

                // Send 1024 Byte
                while (index + CAPABILITY < length) {
                    channel.writeAndFlush(channel.alloc()
                            .buffer(CAPABILITY).writeBytes(bytes, index, CAPABILITY));
                    index += CAPABILITY;
                }
                // Still have data to send.
                if (length - index > 0) {
                    channel.writeAndFlush(channel.alloc()
                            .buffer(length - index).writeBytes(bytes, index, length - index));
                }
            } else {
                channel.writeAndFlush(channel.alloc().buffer(4).writeInt(length));
                channel.writeAndFlush(channel.alloc().buffer(bytes.length).writeBytes(bytes, 0, bytes.length));
            }
            if (writeLog) {
                netLog.log(virtualEap, json, "SEND", DataType.INFORM_REQUEST,
                        (InetSocketAddress) channel.localAddress(), (InetSocketAddress) channel.remoteAddress());
            }
        } else {
            logger.debug("EAP - " + virtualEap.getMac() + " channel is not writable!");

            channel.closeFuture();
            synchronized (virtualEap.lock) {
                virtualEap.setCurrentState(VirtualEap.State.DISCONNECTED);
                virtualEap.lock.notify();
            }
        }
    }

    private void refreshInformRequest(VirtualEap virtualEap, DataHeader header, DataBody body, Packet packet) {
        header.setType(DataType.INFORM_REQUEST);
        header.setMac(virtualEap.getMac());
        header.setError(0);
        // Set DeviceInfo
        Map<String, Object> deviceInfo = new LinkedHashMap<>();
        deviceInfo.put("model", virtualEap.getModel());
        deviceInfo.put("name", virtualEap.getName());
        deviceInfo.put("modelVersion", virtualEap.getModelVersion());
        deviceInfo.put("hardwareVersion", virtualEap.getHardwareVersion());
        deviceInfo.put("firmwareVersion", virtualEap.getFirmwareVersion());
        deviceInfo.put("upTime", virtualEap.getUpTime());
        deviceInfo.put("ip", virtualEap.getIp());
        deviceInfo.put("mask", virtualEap.getMask());
        deviceInfo.put("cpuUti", 1);
        deviceInfo.put("memUti", 71);

        // Clients: empty.
        List<Map<String, Object>> clients = new LinkedList<>();

        // Portal auth clients: empty.
        List<Map<String, Object>> portalAuthClients = new LinkedList<>();

        // Rogue ap list: empty
        List<Map<String, Object>> rogueApList = new LinkedList<>();

        // Trust ap list: empty
        List<Map<String, Object>> trustApList = new LinkedList<>();

        Map<String, Object> radioTraffic_2G = new LinkedHashMap<>();
        radioTraffic_2G.put("rxPackets", 1314898);
        radioTraffic_2G.put("txPackets", 339380);
        radioTraffic_2G.put("rxBytes", 159677138);
        radioTraffic_2G.put("txBytes", 4651712);
        radioTraffic_2G.put("rxDroppedPackets", 0);
        radioTraffic_2G.put("txDroppedPackets", 0);
        radioTraffic_2G.put("rxDroppedBytes", 0);
        radioTraffic_2G.put("txDroppedBytes", 0);
        radioTraffic_2G.put("rxErrors", 0);
        radioTraffic_2G.put("txErrors", 0);

        Map<String, Object> radioTraffic_5G = new LinkedHashMap<>();
        radioTraffic_5G.put("rxPackets", 0);
        radioTraffic_5G.put("txPackets", 0);
        radioTraffic_5G.put("rxBytes", 0);
        radioTraffic_5G.put("txBytes", 0);
        radioTraffic_5G.put("rxDroppedPackets", 0);
        radioTraffic_5G.put("txDroppedPackets", 0);
        radioTraffic_5G.put("rxDroppedBytes", 0);
        radioTraffic_5G.put("txDroppedBytes", 0);
        radioTraffic_5G.put("rxErrors", 0);
        radioTraffic_5G.put("txErrors", 0);

        Map<String, Object> lanTraffic = new LinkedHashMap<>();
        lanTraffic.put("rxPackets", 6621);
        lanTraffic.put("txPackets", 2801);
        lanTraffic.put("rxBytes", 1306740);
        lanTraffic.put("txBytes", 1702955);
        lanTraffic.put("rxDroppedPackets", 0);
        lanTraffic.put("txDroppedPackets", 0);
        lanTraffic.put("rxDroppedBytes", 0);
        lanTraffic.put("txDroppedBytes", 0);
        lanTraffic.put("rxErrors", 0);
        lanTraffic.put("txErrors", 0);

        Map<String, Object> lanInfo = new LinkedHashMap<>();
        lanInfo.put("mac", virtualEap.getMac());
        lanInfo.put("ip", virtualEap.getIp());
        lanInfo.put("netmask", virtualEap.getMask());

        List<Map<String, Object>> systemLog = new LinkedList<>();
        List<Map<String, Object>> ssidStats_2G = new LinkedList<>();
        List<Map<String, Object>> ssidStats_5G = new LinkedList<>();

        Map<String, Object> wSettings_2G = new LinkedHashMap<>();
        wSettings_2G.put("region", 841);
        wSettings_2G.put("channel", "N/A");
        wSettings_2G.put("bandWidth", "20/40MHz");
        wSettings_2G.put("rdMode", "b/g/n mixed");
        wSettings_2G.put("txRate", "54.0Mbps");
        wSettings_2G.put("txPower", "26dBm");

        Map<String, Object> wSettings_5G = new LinkedHashMap<>();
        wSettings_5G.put("region", 841);
        wSettings_5G.put("channel", "N/A");
        wSettings_5G.put("bandWidth", "40/80MHz");
        wSettings_5G.put("rdMode", "a/ac mixed");
        wSettings_5G.put("txRate", "54.0Mbps");
        wSettings_5G.put("txPower", "26dBm");

        List<Map<String, Object>> authedUsers = new LinkedList<>();


        body.put("deviceInfo", deviceInfo);
        body.put("clients", clients);
        body.put("portalAuthClients", portalAuthClients);
        body.put("rogueApList", rogueApList);
        body.put("trustApList", trustApList);
        body.put("radioTraffic_2G", radioTraffic_2G);
        body.put("radioTraffic_5G", radioTraffic_5G);
        body.put("lanTraffic", lanTraffic);
        body.put("lanInfo", lanInfo);
        body.put("systemLog", systemLog);
        body.put("ssidStats_2G", ssidStats_2G);
        body.put("ssidStats_5G", ssidStats_5G);
        body.put("wSettings_2G", wSettings_2G);
        body.put("wSettings_5G", wSettings_5G);
        body.put("authedUsers", authedUsers);
    }

    public EapManageClient setNetLog(NetLog netLog) {
        this.netLog = netLog;
        return this;
    }

    public Map<Integer, VirtualEap> getPortMap() {
        return portMap;
    }

    public VirtualEap getEapByPort(int port) {
        return portMap.get(port);
    }

    class SendInformRequestRunnable implements Runnable {
        @Override
        public void run() {
            while (!stopped) {
                for (Map.Entry<String, VirtualEap> entry : eapMap.entrySet()) {
                    VirtualEap virtualEap = entry.getValue();
                    try {
                        EapManageClient.this.sendInformRequest(virtualEap, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (Thread.interrupted()) {
                    logger.debug("Interrupted send information request loop");
                    break;
                }
                try {
                    Thread.sleep(INFORM_SLEEP_TIME);
                } catch (InterruptedException e) {
                    logger.debug("Interrupted send information request loop");
                    break;
                }
            }
        }
    }
}
