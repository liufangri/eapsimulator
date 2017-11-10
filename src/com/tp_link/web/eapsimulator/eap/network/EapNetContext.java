/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-30
 */

package com.tp_link.web.eapsimulator.eap.network;

import com.tp_link.web.eapsimulator.eap.VirtualEap;
import com.tp_link.web.eapsimulator.eap.network.log.NetLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class EapNetContext {
    private static final Log logger = LogFactory.getLog(EapNetContext.class);

    private EapAdoptionClient adoptionClient;
    private EapDiscoveryClient discoveryClient;
    private EapManageClient manageClient;

    private String serverIP;

    private int adoptPort;
    private int discoveryPort;
    private int managePort;

    private NetLog netLog;
    private Map<String, VirtualEap> virtualEapMap;

    public EapNetContext() {
        this.virtualEapMap = new HashMap<>();
    }

    public void initContext() {
        Properties properties = new Properties();
        try {
            properties.load(this.getClass().getClassLoader().
                    getResourceAsStream("application.properties"));
        } catch (IOException e) {
            logger.error("Can't find resource: 'application.properties'");
        }

        try {
            adoptPort = Integer.parseInt(properties.getProperty("adopt.port"));
            discoveryPort = Integer.parseInt(properties.getProperty("discovery.port"));
            managePort = Integer.parseInt(properties.getProperty("manage.port"));
        } catch (NumberFormatException e) {
            logger.error(e.getMessage());
        }

        // Create all clients.
        adoptionClient = EapAdoptionClient.getInstance(EapAdoptionClient.DEFAULT_PRE_ADOPT_DEST_ADDRESS,
                adoptPort).setNetLog(netLog);
        EapAdoptionHandler adoptionHandler = new EapAdoptionHandler();
        adoptionHandler.setContext(this);
        adoptionHandler.setPublicKeyStr((String) properties.get("public.key"));
        adoptionClient.setAdoptionHandler(adoptionHandler);

        discoveryClient = EapDiscoveryClient.getInstance((String) properties.get("discovery.destAddress"),
                discoveryPort).setNetLog(netLog);
        EapDiscoveryHandler discoveryHandler = new EapDiscoveryHandler();
        discoveryHandler.setContext(this);
        discoveryClient.setDiscoveryHandler(discoveryHandler);

        manageClient = EapManageClient.getInstance(EapManageClient.DEFAULT_MANAGE_ADDRESS, managePort)
                .setNetLog(netLog);
        EapManageHandler manageHandler = new EapManageHandler();
        manageHandler.setNetContext(this);
        manageClient.setManageHandler(manageHandler);
        //TODO: Others ..

//        manageClient = EapManageClient.getInstance(this.virtualEap,
//                EapManageClient.DEFAULT_PRE_CONNECT_DEST_ADDRESS,
//                managePort);
//        EapManageHandler manageHandler = new EapManageHandler();
//        manageHandler.setNetContext(this);
//        manageClient.setManageHandler(manageHandler);
    }

    /**
     * Start all clients.
     */
    public void bootStrapClients() {
        try {
            discoveryClient.run();
            adoptionClient.run();
            manageClient.run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public EapAdoptionClient getAdoptionClient() {
        return adoptionClient;
    }

    public EapDiscoveryClient getDiscoveryClient() {
        return discoveryClient;
    }

    public EapManageClient getManageClient() {
        return manageClient;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
        adoptionClient.setDestAddress(serverIP);
        manageClient.setDestAddress(serverIP);
    }

    public String getServerIP() {
        return this.serverIP;
    }

    public void shutdown() throws InterruptedException {
        if (discoveryClient != null) {
            discoveryClient.stop();
//            discoveryClient.setVirtualEap(null);
            discoveryClient = null;
        }
        if (adoptionClient != null) {
            adoptionClient.stop();
//            discoveryClient.setVirtualEap(null);
            adoptionClient = null;
        }
        if (manageClient != null) {
            manageClient.stop();
            manageClient = null;
        }
    }

    public int getAdoptPort() {
        return adoptPort;
    }

    public int getDiscoveryPort() {
        return discoveryPort;
    }

    public int getManagePort() {
        return managePort;
    }

    public NetLog getNetLog() {
        return netLog;
    }

    public void setNetLog(NetLog netLog) {
        this.netLog = netLog;
    }

    public void startEapBroadcast(VirtualEap virtualEap) {
        discoveryClient.addEap(virtualEap);
        virtualEap.setCurrentState(VirtualEap.State.PENDING);
    }

    public void startEapInform(VirtualEap virtualEap) {
        manageClient.addEap(virtualEap);
    }

    public void stopEapBroadcast(VirtualEap virtualEap) {
        discoveryClient.removeEap(virtualEap);
    }

    public void stopEapInform(VirtualEap virtualEap) {
        manageClient.removeEap(virtualEap);
    }

    public void addVirtualEap(VirtualEap eap) {
        if (eap != null) {
            virtualEapMap.put(eap.getMac(), eap);
        }
    }

    public void removeVirtualEap(VirtualEap eap) {
        virtualEapMap.remove(eap.getMac());
        discoveryClient.removeEap(eap);
        adoptionClient.removeEap(eap);
        manageClient.removeEap(eap);

    }

    public VirtualEap getVirtualEapByMac(String mac) {
        return virtualEapMap.get(mac);
    }

    public boolean openAdoptChannel(VirtualEap eap) throws InterruptedException {
        int port = EapClient.getPort();
        eap.setAdoptPort(port);
        return adoptionClient.connect(eap, serverIP, adoptPort) != null;
    }

    public boolean openManageChannel(VirtualEap eap) throws InterruptedException {
        int port = EapClient.getPort();
        eap.setManagePort(port);
        return manageClient.connect(eap, serverIP, managePort) != null;
    }

    public void doForgetOperation(VirtualEap eap) {
        discoveryClient.removeEap(eap);
        adoptionClient.removeEap(eap);
        manageClient.removeEap(eap);
    }
}
