package com.demo.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.dto.OverviewResponse;
import com.demo.service.DashboardService;
import com.demo.util.TimeUtil;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** Tổng quan cho Dashboard: thống kê, ngưỡng, từng thiết bị kèm số đo mới nhất. */
    @GetMapping("/dashboard/overview")
    public OverviewResponse overview() {
        return dashboardService.overview();
    }

    /** Kiểm tra Server còn sống. */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "serverTime", TimeUtil.format(LocalDateTime.now()));
    }
}
