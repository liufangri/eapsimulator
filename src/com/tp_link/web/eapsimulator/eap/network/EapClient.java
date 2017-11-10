/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-25
 */

package com.tp_link.web.eapsimulator.eap.network;

import com.tp_link.web.eapsimulator.eap.VirtualEap;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class EapClient {
    protected Map<String, VirtualEap> eapMap;
    protected String destAddress;
    protected int destPort;

    private static int currentPort = 0;

    protected EapClient(String destAddress, int destPort) {
        this.destAddress = destAddress;
        this.destPort = destPort;
        eapMap = new ConcurrentHashMap<>();
    }

    public abstract void run() throws InterruptedException;

    public abstract boolean stop() throws InterruptedException;

    public void setDestAddress(String destAddress) {
        this.destAddress = destAddress;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    public Map<String, VirtualEap> getEapMap() {
        return eapMap;
    }

    /**
     * Get an available port, rang from 30000 to 50000.
     *
     * @return An available port.
     * @throws PortUsedUpException If port is used up.
     */
    protected static synchronized int getPort() throws PortUsedUpException {
        int baseNum = 30000;
        int size = 20000;
        boolean checkAgain = true;
        if (currentPort == 0) {
            currentPort = baseNum;
            checkAgain = false;
        }
        while (!isPortAvailable(currentPort)) {
            if (currentPort == baseNum + size) {
                break;
            }
            currentPort++;
        }
        if (currentPort == baseNum + size) {
            currentPort = 0;
            if (checkAgain) {
                currentPort = getPort();
                if (currentPort == 0) {
                    throw new PortUsedUpException("No port available now.");
                } else {
                    return currentPort++;
                }
            } else {
                throw new PortUsedUpException("No port available now.");
            }
        } else {
            return currentPort++;
        }
    }

    /**
     * Check this destPort is occupied or not.
     *
     * @param port The expected port.
     * @return returns <b>true</b> if this destPort is <b>available</b>.
     */
    public static boolean isPortAvailable(int port) {
        try {
            bindPort("127.0.0.1", port);
            bindPort(InetAddress.getLocalHost().getHostAddress(), port);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    private static void bindPort(String host, int port) throws Exception {
        Socket s = new Socket();
        s.bind(new InetSocketAddress(host, port));
        s.close();
    }

    public static class PortUsedUpException extends RuntimeException {
        public PortUsedUpException() {
            super();
        }

        public PortUsedUpException(String msg) {
            super(msg);
        }
    }

    public void addEap(VirtualEap eap) {
        if (eap != null) {
            eapMap.put(eap.getMac(), eap);
        }
    }

    public void removeEap(VirtualEap eap) {
        if (eap != null) {
            eapMap.remove(eap.getMac());
        }
    }
}