/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-25
 */

package com.tp_link.web.eapsimulator.eap.network.protocol;

import java.util.LinkedHashMap;

public class DataHeader extends LinkedHashMap<String, Object> {

    public static final String PROTOCOL_VERSION = "TP-LINK ECS ver 1.0.0";

    public DataHeader() {
        this.put("version", PROTOCOL_VERSION);
        this.put("mac", "");
        this.put("type", DataType.DISCOVERY);
        this.put("error", 0);
    }

    public DataHeader(String version, String mac, int type, int error) {
        this.put("version", version);
        this.put("mac", mac);
        this.put("type", type);
        this.put("error", error);
    }

    public DataHeader setVersion(String version) {
        this.put("version", version);
        return this;
    }

    public DataHeader setMac(String mac) {
        this.put("mac", mac);
        return this;
    }

    public DataHeader setType(int type) {
        this.put("type", type);
        return this;
    }

    public DataHeader setError(int error) {
        this.put("error", error);
        return this;
    }

}
