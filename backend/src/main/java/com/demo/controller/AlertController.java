package com.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo.dto.AlertLogResponse;
import com.demo.dto.PageResponse;
import com.demo.service.AlertService;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /** Nhật ký cảnh báo đã lưu trong MySQL, mới nhất trước. Lọc: deviceId, metric (TEMPERATURE|HUMIDITY). */
    @GetMapping
    public PageResponse<AlertLogResponse> getAlerts(
            @RequestParam(name = "deviceId", required = false) String deviceId,
            @RequestParam(name = "metric", required = false) String metric,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return alertService.search(deviceId, metric, page, size);
    }
}
