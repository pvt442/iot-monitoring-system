package com.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
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
}