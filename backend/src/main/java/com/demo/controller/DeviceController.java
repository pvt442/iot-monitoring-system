package com.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.model.Device;
import com.demo.service.DeviceService;

@RestController
@RequestMapping("/api/devices")
@CrossOrigin("*") // Cực kỳ quan trọng: Cho phép ReactJS gọi API mà không bị chặn lỗi CORS
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    // Cung cấp dữ liệu cho ReactJS hiển thị
    @GetMapping
    public List<Device> getAllDevices() {
        return deviceService.getAllDevices();
    }

    // Nhận lệnh đăng ký thiết bị mới
    @PostMapping
    public Device saveDevice(@RequestBody Device device) {
        return deviceService.saveDevice(device);
    }
}