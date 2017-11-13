/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-20
 */

package com.tp_link.web.eapsimulator.server.po;

import com.tp_link.web.eapsimulator.eap.VirtualEap;

import javax.persistence.*;

@Entity
@Table(name = "EAP_DEVICE")
public class EapDevice {
    /**
     * The primary key
     */
    @Id
    @Column(name = "EAP_ID")
    private String id;

    @Column(name = "EAP_NAME")
    private String deviceName;

    @Column(name = "EAP_MODEL_NAME")
    private String modelName;

    @Column(name = "EAP_MODEL_VERSION")
    private String modelVersion;

    @Column(name = "EAP_SOFTWARE_VERSION")
    private String softwareVersion;

    @Column(name = "EAP_MAC_ADDRESS")
    private String macAddress;

    @Column(name = "EAP_IPV4_ADDRESS")
    private String ipAddress;

    @Column(name = "EAP_CREATE_TIME")
    private long createTime;

    @Column(name = "EAP_MASK")
    private String mask;

    @Column(name = "EAP_USER_NAME")
    private String userName;

    @Column(name = "EAP_PASSWORD_MD5")
    private String passwordMD5;

    @Transient
    private String rc4Key;

    @Transient
    private String state;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getMask() {
        return mask;
    }

    public void setMask(String mask) {
        this.mask = mask;
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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public VirtualEap toVirtualEap() {
        VirtualEap res = new VirtualEap(id);

        res.setCurrentState(VirtualEap.State.NOT_RUNNING);
        res.setMac(macAddress);
        res.setIp(ipAddress);
        res.setModel(modelName);
        res.setName(deviceName);
        res.setFirmwareVersion(softwareVersion);
        res.setHardwareVersion(modelVersion);// TODO: hardware version ???
        res.setModelVersion(modelVersion);
        res.setUserName(userName);
        res.setPasswordMD5(passwordMD5);
        res.setRc4Key(rc4Key);

        return res;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
