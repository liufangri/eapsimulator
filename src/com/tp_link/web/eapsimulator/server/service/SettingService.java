/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-10-12
 */

package com.tp_link.web.eapsimulator.server.service;

import com.tp_link.web.eapsimulator.tools.GsonHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class SettingService {
    private static final Log logger = LogFactory.getLog(SettingService.class);
    private Properties settingProperties;
    private String settingPath;

    public Properties getSettingProperties() {
        return settingProperties;
    }

    public void setSettingProperties(Properties settingProperties) {
        this.settingProperties = settingProperties;
    }

    public void setSettingPath(String settingPath) {
        if (settingProperties == null) {
            settingProperties = new Properties();
        }
        if (settingPath != null && !settingPath.equals(this.settingPath)) {
            try {
                String path = getRealPath(settingPath);
                settingProperties.load(new FileInputStream(path));
                this.settingPath = settingProperties.getProperty("setting.path");
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Get real path according to given relative path, which starts with "WEB-INF".
     *
     * @param relative The relative path.
     * @return The real path
     */
    private String getRealPath(String relative) throws UnresolvablePathPrefix {
        String res;
        String prefix;
        String path;

        if (relative == null) {
            return null;
        }
        if (relative.startsWith("WEB-INF")) {
            prefix = "WEB-INF";
        } else if (relative.startsWith("classes")) {
            prefix = "classes";
        } else {
            throw new UnresolvablePathPrefix("Cannot resolve prefix from resource path " + relative);
        }
        // Get real path
        // path = this.getClass().getResource("").toURI().getPath();
        path = this.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
        if (path.startsWith("zip")) {
            path = path.substring(4);
        } else if (path.startsWith("file")) {
            path = path.substring(6);
        } else if (path.startsWith("jar")) {
            path = path.substring(10);
        }
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }
        res = path.substring(0, path.indexOf(prefix)) +
                relative.substring(relative.indexOf(prefix), relative.length());

        return res;
    }

    public String getSettingPath() {
        return settingPath;
    }

    public String getSettingJson() {
        if (this.settingProperties == null) {
            settingProperties = new Properties();
        }
        return GsonHelper.getGson().toJson(settingProperties);
    }

    public String storeSettings(Map<String, String> parameterMap) {
        if (settingProperties == null) {
            settingProperties = new Properties();
        }
        Set keys = settingProperties.keySet();
        for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (keys.contains(key)) {
                settingProperties.setProperty(key, value);
            } else {
                logger.debug("Received unexpected setting: " + key + "=" + value);
            }
        }
        String path = getRealPath(settingPath);
        try {
            FileOutputStream out = new FileOutputStream(path);
            settingProperties.store(out, "");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return "{\"result\":\"OK\"}";
    }

    public static class UnresolvablePathPrefix extends RuntimeException {
        public UnresolvablePathPrefix(String message) {
            super(message);
        }
    }
}
