/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-25
 */

package com.tp_link.web.eapsimulator.eap.network.protocol;

import java.util.HashMap;
import java.util.Map;

public class Packet {

    /**
     * The Json formed data.
     */
    private Map<String, Object> data = new HashMap<String, Object>();

    public Packet(DataHeader header, DataBody body) {
        data.put("header", header);
        data.put("body", body);
    }

    public Map<String, Object> getData() {
        return data;
    }
}
