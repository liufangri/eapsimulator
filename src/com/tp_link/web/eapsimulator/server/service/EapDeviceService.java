/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-20
 */

package com.tp_link.web.eapsimulator.server.service;

import com.tp_link.web.eapsimulator.server.context.EapDeviceManager;
import com.tp_link.web.eapsimulator.server.dao.EapDeviceDao;
import com.tp_link.web.eapsimulator.server.dao.PagedResult;
import com.tp_link.web.eapsimulator.server.po.EapDevice;
import com.tp_link.web.eapsimulator.tools.GsonHelper;
import io.netty.util.NetUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;

public class EapDeviceService {
    private static final Log logger = LogFactory.getLog(EapDeviceService.class);
    private EapDeviceDao eapDeviceDao;
    private EapDeviceManager eapDeviceManager;
    private SettingService settingService;

    public void setEapDeviceManager(EapDeviceManager eapDeviceManager) {
        this.eapDeviceManager = eapDeviceManager;
    }

    public void setEapDeviceDao(EapDeviceDao eapDeviceDao) {
        this.eapDeviceDao = eapDeviceDao;
    }

    public void setSettingService(SettingService settingService) {
        this.settingService = settingService;
    }

    public List<EapDevice> getEapList() {
        return eapDeviceDao.getDeviceList();
    }

    public String getEapListJson(Map<String, String> map) {
        int limit = Integer.parseInt(map.get("limit"));
        int offset = Integer.parseInt(map.get("offset"));
        PagedResult<EapDevice> result = eapDeviceDao.getAllDeviceListPaged(limit, offset);
        for (EapDevice device : result.getRows()) {
            device.setState(eapDeviceManager.getDeviceStateById(device.getId()));
        }
        String value = GsonHelper.getGson().toJson(result);
        return value;
    }

    public String deleteEapDeviceById(String ids) {
        List<EapDevice> eapDeviceList = eapDeviceDao.getDeviceListByIds(ids);
        int ret = eapDeviceDao.deleteDeviceById(ids);
        if (ret == 0) {
            logger.debug("Delete eap device with id: " + ids + " failed.");
            return "{\"result\":\"Fail\"}";
        } else {
            eapDeviceManager.onEapDevicesDeleted(eapDeviceList);
            logger.debug("Delete eap device with id: " + ids + " success.");
            return "{\"result\":\"OK\"}";
        }
    }

    public String addEapDevice(Map<String, String> parameterMap) {
        String name = parameterMap.get("deviceName");
        String modelVersion = parameterMap.get("modelVersion");
        String softwareVersion = parameterMap.get("softwareVersion");
        String macAddress = parameterMap.get("macAddress").toUpperCase();
        String ipAddress = parameterMap.get("ipAddress");
        String mask = parameterMap.get("mask");
        String modelName = parameterMap.get("modelName");

        String userName = parameterMap.get("userName");
        String password = parameterMap.get("password");

        String id = UUID.randomUUID().toString();

        EapDevice eapDevice = new EapDevice();
        eapDevice.setId(id);
        eapDevice.setModelVersion(modelVersion);
        eapDevice.setSoftwareVersion(softwareVersion);
        eapDevice.setDeviceName(name);
        eapDevice.setMacAddress(macAddress);
        eapDevice.setIpAddress(ipAddress);
        eapDevice.setCreateTime(System.currentTimeMillis());
        eapDevice.setMask(mask);
        eapDevice.setModelName(modelName);

        if (userName == null || "".equals(userName)) {
            eapDevice.setUserName(settingService.getSettingProperties().getProperty("base.default.username"));
        }
        if (password == null || "".equals(password)) {
            eapDevice.setPasswordMD5(settingService.getSettingProperties().getProperty("base.default.password"));
        }
        int ret = eapDeviceDao.addDevice(eapDevice);
        if (ret > 0) {
            List<EapDevice> devices = new ArrayList<>();
            devices.add(eapDevice);
            eapDeviceManager.onEapDevicesAdded(devices);
            return "{\"result\":\"OK\"}";
        } else {
            return "{\"result\":\"Fail\"}";
        }
    }

    public String addBatchDevice(Map<String, String> parameterMap) {
        String res = null;
        int number = Integer.parseInt(parameterMap.get("number"));
        String ipStart = parameterMap.get("ipStart");
        String ipEnd = parameterMap.get("ipEnd");
        String macStart = parameterMap.get("macStart");
        String macEnd = parameterMap.get("macEnd");
        String modelName = parameterMap.get("modelName");
        String modelVersion = parameterMap.get("modelVersion");
        String softwareVersion = parameterMap.get("softwareVersion");

        List<EapDevice> deviceList = new ArrayList<>();
        Properties settings = settingService.getSettingProperties();
        Set<String> ipSet = eapDeviceManager.getEapIpSet();
        Set<String> macSet = eapDeviceManager.getEapMacSet();

        String deviceName = settings.getProperty("base.default.name");
        if (modelName == null) {
            modelVersion = settings.getProperty("base.default.modelName");
        }
        if (softwareVersion == null) {
            softwareVersion = settings.getProperty("base.default.software");
        }
        if (modelVersion == null) {
            modelVersion = settings.getProperty("base.default.modelVersion");
        }
        String mask = settings.getProperty("base.default.mask");

        String userName = settings.getProperty("base.default.username");
        String password = settings.getProperty("base.default.password");

        int i;

        long currentIp = ipToInt(ipStart) & 0xFFFFFFFFL;
        BigInteger currentMac = new BigInteger(macStart.replaceAll(":", ""), 16);

        for (i = 0; i < number; i++) {
            // Get all settings
            String id = UUID.randomUUID().toString();

            // Get an IP based on ip config.
            long ipEndInt = ipToInt(ipEnd) & 0xFFFFFFFFL;
            String ip = null;
            for (; currentIp <= ipEndInt; currentIp++) {
                ip = NetUtil.intToIpAddress((int) currentIp);
                if (!ipSet.contains(ip)) {
                    currentIp++;
                    break;
                }
            }
            if (currentIp == ipEndInt + 1) {
                logger.error("IP address pool is used up!");
                break;
            }

            // Get an mac from mac config.
            String mac = null;
            BigInteger macEndInt = new BigInteger(macEnd.replaceAll(":", ""), 16);
            for (; currentMac.compareTo(macEndInt) <= 0; currentMac = currentMac.add(BigInteger.ONE)) {
                StringBuilder sb = new StringBuilder();
                sb.append(currentMac.toString(16));
                sb.insert(2, ':').insert(5, ':').insert(8, ':').insert(11, ':').insert(14, ':');
                mac = sb.toString().toUpperCase();
                if (!macSet.contains(mac)) {
                    currentMac = currentMac.add(BigInteger.ONE);
                    break;
                }
            }
            if (currentMac.compareTo(macEndInt.add(BigInteger.ONE)) == 0) {
                logger.error("MAC address pool is used up");
                break;
            }

            EapDevice device = new EapDevice();
            device.setId(id);
            device.setUserName(userName);
            device.setPasswordMD5(password);
            device.setCreateTime(System.currentTimeMillis());
            device.setMask(mask);
            device.setDeviceName(deviceName);
            device.setIpAddress(ip);
            device.setMacAddress(mac);
            device.setModelVersion(modelVersion);
            device.setSoftwareVersion(softwareVersion);
            device.setModelName(modelName);
            deviceList.add(device);
        }
        // Check if required number is achieved.
        if (i < number) {
            res = "{\"result\":\"fail\",\"created\":" + i + ",\"message\":\"Not enough Ip or mac.\"}";
        } else {
            eapDeviceDao.addBatchDevice(deviceList);
            eapDeviceManager.onEapDevicesAdded(deviceList);
            res = "{\"result\":\"OK\",\"created\":" + i + "}";
        }
        return res;
    }

    private int byteToInt(byte[] ipStartBytes) {
        return ((((ipStartBytes[0] & 0xFF) << 8 | ipStartBytes[1] & 0xFF) << 8 | ipStartBytes[2] & 0xFF) << 8) |
                ipStartBytes[3] & 0xFF;
    }

    private int ipToInt(String ip) {
        return byteToInt(NetUtil.createByteArrayFromIpAddressString(ip));
    }

    public String modifyEapDevice(Map<String, String> parameterMap) {
        String id = parameterMap.get("id");
        String name = parameterMap.get("deviceName");
        String macAddress = parameterMap.get("macAddress").toUpperCase();
        String ipAddress = parameterMap.get("ipAddress");
        String modelVersion = parameterMap.get("modelVersion");
        String softwareVersion = parameterMap.get("softwareVersion");
        String mask = parameterMap.get("mask");
        String userName = parameterMap.get("userName");
        String password = parameterMap.get("password");

        EapDevice eapDevice = new EapDevice();
        eapDevice.setId(id);
        eapDevice.setModelVersion(modelVersion);
        eapDevice.setSoftwareVersion(softwareVersion);
        eapDevice.setDeviceName(name);
        eapDevice.setMacAddress(macAddress);
        eapDevice.setIpAddress(ipAddress);
        eapDevice.setMask(mask);

        if (userName == null || "".equals(userName)) {
            eapDevice.setUserName(settingService.getSettingProperties().getProperty("base.default.username"));
        }

        // Set password MD5
        if (password == null || "".equals(password)) {
            eapDevice.setPasswordMD5(settingService.getSettingProperties().getProperty("base.default.password"));
        }

        eapDeviceDao.modifyDevice(eapDevice);

        //Modify running eap devices
        eapDeviceManager.onEapDeviceChanged(eapDevice);

        return "{\"result\":\"OK\"}";
    }

    public String collectMultiState(Map<String, String> parameterMap) {
        String idStr = parameterMap.get("idStr");
        String indexStr = parameterMap.get("indexStr");
        if (idStr == null || indexStr == null) {
            return "{\"result\":\"error\"}";
        } else {
            String[] ids = idStr.split(",");
            String[] indexes = indexStr.split(",");
            return collectState(ids, indexes);
        }
    }

    public String collectUniqueState(Map<String, String> parameterMap) {
        String id = parameterMap.get("id");
        String index = parameterMap.get("index");
        String[] ids = {id};
        String[] indexes = {index};

        if (id == null || index == null) {
            return "{\"result\":\"error\"}";
        } else {
            return collectState(ids, indexes);
        }
    }

    private String collectState(String[] ids, String[] indexes) {
        if (ids == null || indexes == null) {
            return "{\"result\":\"error\"}";
        } else {
            String stateJson = eapDeviceManager.getDeviceState(ids, indexes);
            return "{\"result\":\"OK\", \"data\":" + stateJson + "}";
        }
    }

    public String collectRunningNum() {
        return "{\"result\":\"OK\", \"data\":{\"num\":" + eapDeviceManager.getRunningNum() + "}}";
    }

    public String startEapById(Map<String, String> map) {
        String id = map.get("idStr");
        int ret = eapDeviceManager.startEapByIds(id);
        if (ret >= 0) {
            return "{\"result\":\"OK\"}";
        } else {
            return "{\"result\":\"fail\"}";
        }
    }

    public String stopEapById(Map<String, String> map) {
        String id = map.get("idStr");
        int ret = eapDeviceManager.stopEapByIds(id);
        if (ret >= 0) {
            return "{\"result\":\"OK\"}";
        } else {
            return "{\"result\":\"fail\"}";
        }
    }
}
