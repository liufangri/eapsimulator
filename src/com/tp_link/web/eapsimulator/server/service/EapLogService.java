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

import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class EapLogService implements NetLog {
    private static final Log logger = LogFactory.getLog(EapLogService.class);
    private EapLogDao eapLogDao;


    public void setEapLogDao(EapLogDao eapLogDao) {
        this.eapLogDao = eapLogDao;
    }

    @Override
    public NetLog log(VirtualEap virtualEap, String comment, String action, int type,
                      String sourceAddress, int sourcePort, String destAddress, int destPort) {
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

//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                eapLogDao.saveLog(log);
//            }
//        });
//        thread.setName("Log_save");
        eapLogDao.saveLog(log);
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
//
//    public static class LogQueue {
//        public static final int QUEUE_LIMIT = 1000;
//        private EapLog[] logQueue = new EapLog[QUEUE_LIMIT];
//        private int current = 0;
//        private int read = current;
//
//        public synchronized void addEapLog(EapLog log) {
//            current++;
//            if (current == QUEUE_LIMIT) {
//                current = 0;
//            }
//            while (current == read) {
//                try {
//                    wait(100);
//                } catch (InterruptedException e) {
//
//                }
//            }
//
//        }
//
//    }
}
