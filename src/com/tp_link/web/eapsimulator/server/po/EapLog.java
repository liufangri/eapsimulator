/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-10-19
 */

package com.tp_link.web.eapsimulator.server.po;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "EAP_LOG")
public class EapLog {
    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "EAP_ID")
    private String eapId;

    @Column(name = "COMMENT")
    private String comment;

    @Column(name = "CREATE_TIME")
    private long createTime;

    @Column(name = "ACTION")
    private String action;

    @Column(name = "DEST_ADDRESS")
    private String destAddress;

    @Column(name = "DEST_PORT")
    private int destPort;

    @Column(name = "SOURCE_ADDRESS")
    private String sourceAddress;

    @Column(name = "SOURCE_PORT")
    private int sourcePort;

    @Column(name = "EAP_MAC")
    private String eapMac;

    @Column(name = "TYPE")
    private int type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEapId() {
        return eapId;
    }

    public void setEapId(String eapId) {
        this.eapId = eapId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDestAddress() {
        return destAddress;
    }

    public void setDestAddress(String destAddress) {
        this.destAddress = destAddress;
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public String getEapMac() {
        return eapMac;
    }

    public void setEapMac(String eapMac) {
        this.eapMac = eapMac;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
