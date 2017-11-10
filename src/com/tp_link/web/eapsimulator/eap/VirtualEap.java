/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-25
 */

package com.tp_link.web.eapsimulator.eap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Formatter;

/**
 * Contains attributes of EAP devices
 */
public class VirtualEap {
    private static final Log logger = LogFactory.getLog(VirtualEap.class);

    /**
     * The state of this virtual eap.<br/>
     * Including the flowing states:<br/>
     * INIT,
     * PENDING,
     * CONNECTING,
     * CONNECTED,
     * DISCONNECTED.
     */
    public enum State {
        INIT,
        PENDING,
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        NOT_RUNNING,
        PROVISIONING,
        CONFIGURING
    }

    /**
     * The primary key of eap device
     */
    private String id;

    private State currentState;

    // device info
    private String mac;
    private String ip;
    private String mask;
    private String model;
    private String name;
    private String firmwareVersion;
    private String modelVersion;
    private String hardwareVersion;

    // device login info
    private String userName;
    private String passwordMD5;

    // device misc
    private boolean support_5g = false;
    private boolean support_11ac = false;
    private boolean support_lag = false;
    private int customizeRegion = 841;
    private int minPower2G = 4;
    private int maxPower2G = 26;
    private int minPower5G = 11;
    private int maxPower5G = 23;

    // Run time info
    private long startTimeMillis = System.currentTimeMillis();

    /**
     * The RC4 key encrypted by base64.
     */
    private String rc4Key;

    private int adoptPort;
    private int managePort;

    public VirtualEap(String id) {
        this.id = id;
    }


    public final Object lock = new Object();

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public String getUpTime() {
        long current = System.currentTimeMillis();
        long diff = current - startTimeMillis;
        long secondLen = 1000L;
        long minLen = 60L * secondLen;
        long hourLen = 60L * minLen;
        long dayLen = 24L * hourLen;
        long days = diff / dayLen;
        long hour = diff % dayLen / hourLen;
        long min = diff % hourLen / minLen;
        long second = diff % minLen / secondLen;
        StringBuffer stringBuffer = new StringBuffer();
        Formatter formatter = new Formatter(stringBuffer);
        String str = formatter.format("%d days %02d:%02d:%02d", days, hour, min, second).toString();
        return str;
    }

    public String getId() {
        return id;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMask() {
        return mask;
    }

    public void setMask(String mask) {
        this.mask = mask;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public boolean getSupport_5g() {
        return support_5g;
    }

    public void setSupport_5g(boolean support_5g) {
        this.support_5g = support_5g;
    }

    public boolean getSupport_11ac() {
        return support_11ac;
    }

    public void setSupport_11ac(boolean support_11ac) {
        this.support_11ac = support_11ac;
    }

    public boolean getSupport_lag() {
        return support_lag;
    }

    public void setSupport_lag(boolean support_lag) {
        this.support_lag = support_lag;
    }

    public int getCustomizeRegion() {
        return customizeRegion;
    }

    public void setCustomizeRegion(int customizeRegion) {
        this.customizeRegion = customizeRegion;
    }

    public int getMinPower2G() {
        return minPower2G;
    }

    public void setMinPower2G(int minPower2G) {
        this.minPower2G = minPower2G;
    }

    public int getMaxPower2G() {
        return maxPower2G;
    }

    public void setMaxPower2G(int maxPower2G) {
        this.maxPower2G = maxPower2G;
    }

    public int getMinPower5G() {
        return minPower5G;
    }

    public void setMinPower5G(int minPower5G) {
        this.minPower5G = minPower5G;
    }

    public int getMaxPower5G() {
        return maxPower5G;
    }

    public void setMaxPower5G(int maxPower5G) {
        this.maxPower5G = maxPower5G;
    }

    public State getCurrentState() {
        return currentState;
    }

    synchronized public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPasswordMD5() {
        return passwordMD5;
    }

    public void setPasswordMD5(String passwordMD5) {
        this.passwordMD5 = passwordMD5;
    }

    public String getRc4Key() {
        return rc4Key;
    }

    public void setRc4Key(String rc4Key) {
        this.rc4Key = rc4Key;
    }

    public static Log getLogger() {
        return logger;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getHardwareVersion() {
        return hardwareVersion;
    }

    public void setHardwareVersion(String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    public int getAdoptPort() {
        return adoptPort;
    }

    public void setAdoptPort(int adoptPort) {
        this.adoptPort = adoptPort;
    }

    public int getManagePort() {
        return managePort;
    }

    public void setManagePort(int managePort) {
        this.managePort = managePort;
    }
}
