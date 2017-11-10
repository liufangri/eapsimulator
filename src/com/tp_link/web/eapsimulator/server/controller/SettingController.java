/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-10-12
 */

package com.tp_link.web.eapsimulator.server.controller;

import com.tp_link.web.eapsimulator.server.service.SettingService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

@Controller
public class SettingController {

    private static final Log logger = LogFactory.getLog(SettingController.class);
    private SettingService settingService;

    @RequestMapping(path = "getSettings")
    public void getSettings(@RequestParam Map<String, String> parameterMap, Writer writer) throws IOException {
        String res = settingService.getSettingJson();
        writer.write(res);
    }

    @RequestMapping(path = "storeSettings")
    public void storeSettings(@RequestParam Map<String, String> parameterMap, Writer writer) throws IOException {
        String res = settingService.storeSettings(parameterMap);
        writer.write(res);
    }

    public void setSettingService(SettingService settingService) {
        this.settingService = settingService;
    }
}
