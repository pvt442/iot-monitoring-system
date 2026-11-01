package com.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.demo.model.Device;
import com.demo.repository.DeviceRepository;

@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;

    // Lấy danh sách toàn bộ thiết bị
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    // Thêm mới hoặc cập nhật thiết bị
    public Device saveDevice(Device device) {
        return deviceRepository.save(device);
    }
}