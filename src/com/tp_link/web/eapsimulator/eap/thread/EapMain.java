/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-25
 */

package com.tp_link.web.eapsimulator.eap.thread;

import com.tp_link.web.eapsimulator.eap.VirtualEap;
import com.tp_link.web.eapsimulator.eap.network.EapNetContext;
import com.tp_link.web.eapsimulator.eap.network.log.NetLog;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataBody;
import com.tp_link.web.eapsimulator.eap.network.protocol.DataHeader;
import com.tp_link.web.eapsimulator.eap.network.protocol.Packet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Field;

public class EapMain implements Runnable {
    private static final Log logger = LogFactory.getLog(EapMain.class);
    private EapNetContext netContext;
    private VirtualEap virtualEap;
    private boolean terminated = true;
    private Thread thread;

    public EapMain(VirtualEap virtualEap, EapNetContext netContext) {
        this.virtualEap = virtualEap;
        this.netContext = netContext;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public VirtualEap getVirtualEap() {
        return virtualEap;
    }

    @Override
    public void run() {
        terminated = false;
        netContext.addVirtualEap(virtualEap);
        virtualEap.setCurrentState(VirtualEap.State.INIT);
        while (!terminated) {
            switch (virtualEap.getCurrentState()) {
                case INIT:
                    //TODO: Check broadcast state.
                    netContext.startEapBroadcast(virtualEap);
                    synchronized (virtualEap.lock) {
                        try {
                            virtualEap.lock.wait(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    break;
                case PENDING:
                    try {
                        synchronized (virtualEap.lock) {
                            virtualEap.lock.wait();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    break;
                case CONNECTING:
                    try {
                        if (netContext.openAdoptChannel(virtualEap)) {
                            netContext.getAdoptionClient().sendPreConnectInformation(virtualEap);
                            synchronized (virtualEap.lock) {
                                virtualEap.lock.wait(32000);
                                if (virtualEap.getCurrentState() == VirtualEap.State.CONNECTING) {
                                    virtualEap.setCurrentState(VirtualEap.State.INIT);
                                    break;
                                }
                            }
                        } else {
                            virtualEap.setCurrentState(VirtualEap.State.INIT);
                            break;
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    break;
                case PROVISIONING:
                    try {
                        logger.debug("Adoption of " + virtualEap.getId().substring(0, 8) + " FINISHED!");
                        Thread.sleep(2000);
                        if (netContext.openManageChannel(virtualEap)) {
                            netContext.getManageClient().sendPreConnectInformation(virtualEap);
                            Thread.sleep(1000);
                            DataHeader header = new DataHeader();
                            DataBody body = new DataBody();
                            netContext.getManageClient().sendInformRequest(header, body, new Packet(header, body),
                                    virtualEap, true);
                            synchronized (virtualEap.lock) {
                                virtualEap.lock.wait(180000);
                                if (virtualEap.getCurrentState() == VirtualEap.State.PROVISIONING) {
                                    virtualEap.setCurrentState(VirtualEap.State.INIT);
                                    break;
                                }
                            }
                        } else {
                            virtualEap.setCurrentState(VirtualEap.State.INIT);
                            break;
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();

                    }
                    break;
                case CONFIGURING:
                    try {
                        synchronized (virtualEap.lock) {
                            virtualEap.lock.wait(120000);
                            if (virtualEap.getCurrentState() == VirtualEap.State.CONFIGURING) {
                                netContext.getManageClient().stopSendInform(virtualEap);
                                virtualEap.setCurrentState(VirtualEap.State.INIT);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();

                    }
                    break;
                case CONNECTED:
                    try {
                        netContext.startEapInform(virtualEap);
                        netContext.stopEapBroadcast(virtualEap);
                        synchronized (virtualEap.lock) {
                            virtualEap.lock.wait();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                case NOT_RUNNING:
                    break;
                case DISCONNECTED:
                    netContext.doForgetOperation(virtualEap);
                    virtualEap.setCurrentState(VirtualEap.State.INIT);
                    break;
                default:
                    break;
            }
            if (Thread.interrupted()) {
                logger.debug("Terminating main thread of EAP-" + virtualEap.getId().substring(0, 8));
                break;
            }
        }
        terminated = true;
    }

    /**
     * Set all of virtual eap field with new values.
     *
     * @param virtualEap The new virtual eap object
     */
    public synchronized void updateVirtualEap(VirtualEap virtualEap) {
        Field[] fields = virtualEap.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                field.set(this.virtualEap, field.get(virtualEap));
            } catch (IllegalAccessException e) {
            }
        }
    }

    public void terminate() {
        terminated = true;
        //TODO: kill all clients and undergoing stuffs.
        if (virtualEap != null) {
            virtualEap.setCurrentState(VirtualEap.State.NOT_RUNNING);
        }
        if (netContext != null) {
            netContext.removeVirtualEap(virtualEap);
        }
        if (thread != null) {
            thread.interrupt();
        }
    }

    public boolean isRunning() {
        return !terminated;
    }

}
