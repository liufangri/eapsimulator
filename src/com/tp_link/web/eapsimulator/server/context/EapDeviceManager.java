/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-25
 */

package com.tp_link.web.eapsimulator.server.context;

import com.tp_link.web.eapsimulator.eap.VirtualEap;
import com.tp_link.web.eapsimulator.eap.network.EapNetContext;
import com.tp_link.web.eapsimulator.eap.thread.EapMain;
import com.tp_link.web.eapsimulator.server.service.EapLogService;
import com.tp_link.web.eapsimulator.server.dao.EapDeviceDao;
import com.tp_link.web.eapsimulator.server.po.EapDevice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;

@Component
public class EapDeviceManager {
    private static final Log logger = LogFactory.getLog(EapDeviceManager.class);
    private EapDeviceDao eapDeviceDao;
    private EapLogService eapLogService;
    private ThreadPoolTaskExecutor taskExecutor;
    private Map<String, EapDevice> eapDeviceMap;
    private Map<String, VirtualEap> virtualEapMap;
    private Map<String, EapMain> eapMainMap;
    private Set<String> eapIpSet;
    private Set<String> eapMacSet;
    private EapNetContext eapNetContext;

    public void setEapLogService(EapLogService eapLogService) {
        this.eapLogService = eapLogService;
    }

    public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void setEapDeviceDao(EapDeviceDao eapDeviceDao) {
        this.eapDeviceDao = eapDeviceDao;
    }

    public Set<String> getEapIpSet() {
        return eapIpSet;
    }

    public Set<String> getEapMacSet() {
        return eapMacSet;
    }

    /**
     * Init this manager
     */
    @PostConstruct
    public void init() {
        logger.info("Refresh Eap list.");
        taskExecutor.setThreadNamePrefix("EAP-Main-");
        refreshEapMap();
        eapNetContext = new EapNetContext();
        eapNetContext.setNetLog(eapLogService);
        eapNetContext.initContext();
        eapNetContext.bootStrapClients();
    }

    /**
     * Close all eap virtual device
     */
    @PreDestroy
    public void terminate() {
        if (eapMainMap != null) {
            for (Map.Entry<String, EapMain> entry : eapMainMap.entrySet()) {
                EapMain eapMain = entry.getValue();
                eapMain.terminate();
            }
        }
        try {
            eapNetContext.shutdown();
        } catch (InterruptedException e) {

        }
    }

    public int startEapByIds(String idStr) {
        if (idStr != null) {
            String[] ids = idStr.split(",");
            int i = 0;
            for (String id : ids) {
                EapMain eapMain = eapMainMap.get(id);
                if (eapMainMap != null && eapMain == null) {
                    VirtualEap virtualEap = virtualEapMap.get(id);
                    if (virtualEap != null) {
                        eapMain = new EapMain(virtualEap, eapNetContext);
                        eapMainMap.put(virtualEap.getId(), eapMain);
                        virtualEap.setCurrentState(VirtualEap.State.INIT);
                        startEapMainThread(eapMain);
                    }
                } else if (eapMain != null) {
                    if (!eapMain.isRunning()) {
                        eapMain.getVirtualEap().setCurrentState(VirtualEap.State.INIT);
                        startEapMainThread(eapMain);
                    }
                }
                i++;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.debug("wait for start interrupted.");
            }
            return i;
        } else {
            logger.debug("Can't start any eap. ID is null!");
            return -1;
        }
    }

    public int stopEapByIds(String idStr) {
        if (idStr != null) {
            String[] ids = idStr.split(",");
            int i = 0;
            for (String id : ids) {
                EapMain eapMain = eapMainMap.get(id);
                if (eapMain != null) {
                    eapMain.terminate();
                }
                eapMainMap.remove(id);
                i++;
            }
            return i;
        } else {
            logger.error("Can't stop any eap. ID is null!");
            return -1;
        }
    }

    /**
     * Start all eap devices in eap map
     */
    public void startAll() {
        for (Map.Entry<String, VirtualEap> virtualEapEntry : virtualEapMap.entrySet()) {
            VirtualEap virtualEap = virtualEapEntry.getValue();
            EapMain eapMain = new EapMain(virtualEap, eapNetContext);
            eapMainMap.put(virtualEap.getId(), eapMain);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
            startEapMainThread(eapMain);
        }
    }

    private void startEapMainThread(EapMain mainThread) {
        Thread thread = taskExecutor.createThread(mainThread);
        mainThread.setThread(thread);
        thread.start();
    }

    /**
     * Refresh eap maps
     */
    private void refreshEapMap() {
        if (eapDeviceMap == null) {
            eapDeviceMap = new HashMap<>();
        } else {
            eapDeviceMap.clear();
        }

        if (virtualEapMap == null) {
            virtualEapMap = new HashMap<>();
        } else {
            virtualEapMap.clear();
        }

        if (eapMainMap == null) {
            eapMainMap = new HashMap<>();
        } else {
            eapMainMap.clear();
        }

        if (eapIpSet == null) {
            eapIpSet = new HashSet<>();
        } else {
            eapIpSet.clear();
        }
        if (eapMacSet == null) {
            eapMacSet = new HashSet<>();
        } else {
            eapMacSet.clear();
        }

        //Retreat eap devices list
        List<EapDevice> eapDeviceList = eapDeviceDao.getDeviceList();

        for (EapDevice eapDevice : eapDeviceList) {
            eapDeviceMap.put(eapDevice.getId(), eapDevice);
            virtualEapMap.put(eapDevice.getId(), eapDevice.toVirtualEap());
            eapIpSet.add(eapDevice.getIpAddress());
            eapMacSet.add(eapDevice.getMacAddress());
        }
    }

    /**
     * Execute after EapDevice is added
     *
     * @param eapDeviceList The list of new eapDevice.
     */
    public void onEapDevicesAdded(List<EapDevice> eapDeviceList) {
        if (eapDeviceList != null && !eapDeviceList.isEmpty()) {
            for (EapDevice eapDevice : eapDeviceList) {
                String id = eapDevice.getId();
                eapDeviceMap.put(id, eapDevice);
                VirtualEap virtualEap = eapDevice.toVirtualEap();
                virtualEapMap.put(id, virtualEap);

                eapIpSet.add(eapDevice.getIpAddress());
                eapMacSet.add(eapDevice.getMacAddress());

                // Add a eap main thread.
//                EapMain eapMainThread = new EapMain(virtualEap);
//                eapMainThread.setNetLog(eapLogService);
//                eapMainMap.put(virtualEap.getId(), eapMainThread);
//                startEapMainThread(eapMainThread);
            }
        }
    }

    /**
     * Execute after EapDevice is deleted.
     *
     * @param eapDeviceList The list of deleted eapDevice.
     */
    public void onEapDevicesDeleted(List<EapDevice> eapDeviceList) {
        if (eapDeviceList != null && !eapDeviceList.isEmpty()) {
            for (EapDevice eapDevice : eapDeviceList) {
                String id = eapDevice.getId();
                EapMain eapMain = eapMainMap.get(id);
                if (eapMain != null) {
                    eapMain.terminate();
                }
                eapMainMap.remove(id);
                eapDeviceMap.remove(id);
                virtualEapMap.remove(id);
                eapIpSet.remove(eapDevice.getIpAddress());
                eapMacSet.remove(eapDevice.getMacAddress());
            }
        }
    }

    /**
     * When eap device is modified.
     *
     * @param eapDevice
     */
    public void onEapDeviceChanged(EapDevice eapDevice) {
        String id = eapDevice.getId();
        eapDeviceMap.remove(id);
        virtualEapMap.remove(id);

        VirtualEap virtualEap = eapDevice.toVirtualEap();
        eapDeviceMap.put(id, eapDevice);
        virtualEapMap.put(id, virtualEap);

        eapIpSet.add(eapDevice.getIpAddress());
        eapMacSet.add(eapDevice.getMacAddress());
        EapMain eapMain = eapMainMap.get(id);
        if (eapMain != null) {
            eapMain.updateVirtualEap(virtualEap);
        }
    }

    public String getDeviceState(String[] ids, String[] indexes) {
        if (ids.length != indexes.length) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        for (int i = 0; i < ids.length; i++) {
            EapMain eapMain = eapMainMap.get(ids[i]);
            builder.append("{\"id\":\"");
            builder.append(ids[i]);
            builder.append("\",\"state\":\"");
            if (eapMain == null) {
                builder.append(VirtualEap.State.NOT_RUNNING.toString());
            } else {
                builder.append(eapMain.getVirtualEap().getCurrentState());
            }
            builder.append("\", \"index\":");
            builder.append(indexes[i]);
            builder.append("},");
        }
        if (builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Get device state.
     *
     * @param id the device id
     * @return the state string.
     */
    public String getDeviceStateById(String id) {
        EapMain eapMain = eapMainMap.get(id);
        if (eapMain == null) {
            return VirtualEap.State.NOT_RUNNING.toString();
        } else {
            return eapMain.getVirtualEap().getCurrentState().toString();
        }
    }

    /**
     * Get the number of running eap devices
     *
     * @return Running eap number.
     */
    public int getRunningNum() {
        int i = 0;
        for (Map.Entry<String, EapMain> entry : eapMainMap.entrySet()) {
            EapMain eapMain = entry.getValue();
            if (eapMain != null && eapMain.isRunning()) {
                i++;
            }
        }
        return i;
    }
}
