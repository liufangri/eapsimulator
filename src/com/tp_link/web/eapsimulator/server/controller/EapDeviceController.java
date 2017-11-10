/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-20
 */

package com.tp_link.web.eapsimulator.server.controller;

import com.tp_link.web.eapsimulator.server.service.EapDeviceService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

@Controller
public class EapDeviceController {
    private static final Log logger = LogFactory.getLog(EapDeviceController.class);
    private EapDeviceService eapDeviceService;

    public void setEapDeviceService(EapDeviceService eapDeviceService) {
        this.eapDeviceService = eapDeviceService;
    }

    @RequestMapping(path = "getEapDevJson")
    public void getEapDeviceJSON(@RequestParam Map<String, String> parameterMap, Writer writer) throws IOException {
        // Get virtual eap device list
        String result = eapDeviceService.getEapListJson(parameterMap);
        writer.write(result);
    }

    @RequestMapping(path = "addEapDev")
    public void addEapDevice(@RequestParam Map<String, String> parameterMap, Writer writer) throws IOException {
        String result = eapDeviceService.addEapDevice(parameterMap);
        writer.write(result);
    }

    @RequestMapping(path = "delEapDevById")
    public void deleteEapDeviceById(@RequestParam(name = "deviceIds") String ids, Writer writer) throws IOException {
        String result = eapDeviceService.deleteEapDeviceById(ids);
        writer.write(result);
    }

    @RequestMapping(path = "modifyEapDevice")
    public void modifyEapDevice(@RequestParam Map<String, String> parameterMap, Writer writer) throws IOException {
        String result = eapDeviceService.modifyEapDevice(parameterMap);
        writer.write(result);
    }

    @RequestMapping(path = "addBatch")
    public void addBachDevice(@RequestParam Map<String, String> parameterMap, Writer writer) throws IOException {
        String result = eapDeviceService.addBatchDevice(parameterMap);
        writer.write(result);
    }

    @RequestMapping(path = "collectUniqueState")
    public void collectUniqueState(@RequestParam Map<String, String> parameterMap, Writer writer) throws IOException {
        String result = eapDeviceService.collectUniqueState(parameterMap);
        writer.write(result);
    }

    @RequestMapping(path = "collectDeviceState")
    public void collectDeviceState(@RequestParam Map<String, String> parameterMap, Writer writer) throws IOException {
        String result = eapDeviceService.collectMultiState(parameterMap);
        writer.write(result);
    }

    @RequestMapping(path = "collectRunningNum")
    public void collectRunningNum(Writer writer) throws IOException {
        String result = eapDeviceService.collectRunningNum();
        writer.write(result);
    }

    @RequestMapping(path = "startEap")
    public void startEap(@RequestParam Map<String, String> parameterMap, Writer writer) throws IOException {
        String result = eapDeviceService.startEapById(parameterMap);
        writer.write(result);
    }

    @RequestMapping(path = "stopEap")
    public void stopEap(@RequestParam Map<String, String> parameterMap, Writer writer) throws IOException {
        String result = eapDeviceService.stopEapById(parameterMap);
        writer.write(result);
    }
}
