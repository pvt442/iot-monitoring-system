package com.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.model.SensorData;
import com.demo.service.SensorDataService;

@RestController
@RequestMapping("/api/sensor-data")
@CrossOrigin("*")
public class SensorDataController {

    @Autowired
    private SensorDataService sensorDataService;

    // Điểm cuối (Endpoint) để con giả lập Python bắn dữ liệu vào
    @PostMapping
    public SensorData receiveSensorData(@RequestBody SensorData data) {
        return sensorDataService.saveSensorData(data);
    }
    @GetMapping
    public List<SensorData> getAllHistory() {
        return sensorDataService.getAllHistory();
    }

    // 1. API Lấy lịch sử cảm biến của 1 thiết bị (http://localhost:8080/api/sensor-data/DEV01/history)
    @GetMapping("/{deviceId}/history")
    public List<SensorData> getHistoryData(@PathVariable String deviceId) {
        return sensorDataService.getHistoryByDevice(deviceId);
    }

    // 2. API Lấy dữ liệu mới nhất của 1 thiết bị (http://localhost:8080/api/sensor-data/DEV01/latest)
    @GetMapping("/{deviceId}/latest")
    public SensorData getLatestData(@PathVariable String deviceId) {
        return sensorDataService.getLatestByDevice(deviceId);
    }
}