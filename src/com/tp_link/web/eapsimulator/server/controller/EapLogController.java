/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-10-19
 */

package com.tp_link.web.eapsimulator.server.controller;

import com.tp_link.web.eapsimulator.server.service.EapLogService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

@Controller
public class EapLogController {
    private static final Log logger = LogFactory.getLog(EapLogController.class);

    private EapLogService eapLogService;

    @RequestMapping(path = "getAllLogs")
    public void getAllEapLogs(@RequestParam Map<String, String> parameterMap, Writer writer) throws IOException {
        String res = eapLogService.getAllEapLogs(parameterMap);
        writer.write(res);
    }

    @RequestMapping(path = "getLogsByDeviceId")
    public void getEapLogs(@RequestParam Map<String, String> parameterMap, Writer writer) throws IOException {
        String res = eapLogService.getEapLogsByDeviceId(parameterMap);
        writer.write(res);
    }

    public void setEapLogService(EapLogService eapLogService) {
        this.eapLogService = eapLogService;
    }
}
