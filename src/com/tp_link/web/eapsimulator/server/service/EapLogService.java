/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-10-19
 */

package com.tp_link.web.eapsimulator.server.service;

import com.tp_link.web.eapsimulator.eap.VirtualEap;
import com.tp_link.web.eapsimulator.eap.network.log.NetLog;
import com.tp_link.web.eapsimulator.server.dao.EapLogDao;
import com.tp_link.web.eapsimulator.server.dao.PagedResult;
import com.tp_link.web.eapsimulator.server.po.EapLog;
import com.tp_link.web.eapsimulator.tools.GsonHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EapLogService implements NetLog {
    private static final Log logger = LogFactory.getLog(EapLogService.class);
    private boolean logActive = false;
    private SettingService settingService;
    private EapLogDao eapLogDao;
    private ConcurrentLinkedQueue<EapLog> logQueue = new ConcurrentLinkedQueue<>();
    private LogSaveTask logSaveTask = new LogSaveTask();
    private Thread logSaveThread;

    public void setSettingService(SettingService settingService) {
        this.settingService = settingService;
    }

    public void setEapLogDao(EapLogDao eapLogDao) {
        this.eapLogDao = eapLogDao;
    }

    @PostConstruct
    public void initLogSetting() {
        logSaveThread = new Thread(logSaveTask);
        logSaveThread.setName("EAP-Log");
        logSaveThread.start();
        logActive = Boolean.parseBoolean(settingService.getSettingByName("eap.netlog.active"));
    }

    @Override
    public NetLog log(VirtualEap virtualEap, String comment, String action, int type,
                      String sourceAddress, int sourcePort, String destAddress, int destPort) {
        if (logActive) {
            EapLog log = new EapLog();
            log.setComment(comment);
            log.setAction(action);
            log.setEapId(virtualEap.getId());
            log.setSourceAddress(sourceAddress);
            log.setSourcePort(sourcePort);
            log.setDestAddress(destAddress);
            log.setDestPort(destPort);
            log.setId(UUID.randomUUID().toString());
            log.setCreateTime(System.currentTimeMillis());
            log.setEapMac(virtualEap.getMac());
            log.setType(type);
            logQueue.add(log);
            logger.debug("Added log into log queue.");
        }
        return this;
    }

    @Override
    public NetLog log(Object o, String comment, String action) {
        // Do nothing ..
        return this;
    }

    public String getAllEapLogs(Map<String, String> params) {
        int offset = Integer.valueOf(params.get("offset"));
        int limit = Integer.valueOf(params.get("limit"));
        PagedResult<EapLog> logs = eapLogDao.getLogPage(limit, offset);
        String res = GsonHelper.getGson().toJson(logs);
        return res;
    }

    public String getEapLogsByDeviceId(Map<String, String> params) {
        int limit = Integer.parseInt(params.get("limit"));
        int offset = Integer.parseInt(params.get("offset"));
        String deviceId = params.get("deviceId");
        PagedResult<EapLog> result = eapLogDao.getLogPageByDeviceId(limit, offset, deviceId);
        return GsonHelper.getGson().toJson(result);
    }

    public void refreshLogSetting() {
        logActive = Boolean.parseBoolean(settingService.getSettingByName("eap.netlog.active"));
    }

    private class LogSaveTask implements Runnable {
        List<EapLog> logs = new LinkedList<>();

        @Override
        public void run() {
            while (true) {
                int currentSize = logQueue.size();
                if (currentSize >= 50) {
                    for (int i = 0; i < 50; i++) {
                        logs.add(logQueue.poll());
                    }
                    try {
                        eapLogDao.saveLog(logs);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error(e.getMessage());
                    }
                    logs.clear();
                } else {
                    EapLog eapLog = logQueue.poll();
                    if (eapLog != null) {
                        try {
                            eapLogDao.saveLog(eapLog);
                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.error(e.getMessage());
                        }
                    } else {
                        try {
                            Thread.sleep(40);
                        } catch (InterruptedException e) {
                            logger.debug("Log thread interrupted!");
                            break;
                        }
                    }
                }
                if (Thread.currentThread().isInterrupted()) {
                    logger.debug("Log thread interrupted!");
                    break;
                }

            }
        }
    }

    @PreDestroy
    public void stop() {
        logSaveThread.interrupt();
    }
}
