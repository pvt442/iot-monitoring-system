package com.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.dto.DeviceRequest;
import com.demo.dto.DeviceResponse;
import com.demo.service.DeviceService;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /** Danh sách thiết bị kèm trạng thái Online/Offline. */
    @GetMapping
    public List<DeviceResponse> getAllDevices() {
        return deviceService.getAllDevices();
    }

    @GetMapping("/{deviceId}")
    public DeviceResponse getDevice(@PathVariable("deviceId") String deviceId) {
        return deviceService.getDevice(deviceId);
    }

    /** Đăng ký thiết bị mới (201) hoặc cập nhật thiết bị đã có (200). */
    @PostMapping
    public ResponseEntity<DeviceResponse> saveDevice(@RequestBody DeviceRequest request) {
        DeviceService.SaveResult result = deviceService.saveDevice(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.device());
    }

    /** Xóa thiết bị và toàn bộ dữ liệu liên quan. */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteDevice(@PathVariable("deviceId") String deviceId) {
        deviceService.deleteDevice(deviceId);
        return ResponseEntity.noContent().build();
    }
}
