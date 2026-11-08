package com.demo.dto;

/**
 * Body của POST /api/sensor-data. Chấp nhận cả hai cách viết deviceId:
 *   {"deviceId":"DEV_01","temperature":25.0,"humidity":60.0,"timestamp":"2026-11-07T10:30:00"}   (khuyến nghị)
 *   {"device":{"deviceId":"DEV_01"},"temperature":25.0,"humidity":60.0}                          (cấu trúc cũ)
 * timestamp là tùy chọn; thiếu thì Server dùng giờ nhận.
 */
public record SensorDataRequest(
        String deviceId,
        DeviceRef device,
        Double temperature,
        Double humidity,
        String timestamp) {

    public record DeviceRef(String deviceId) {
    }

    public String resolveDeviceId() {
        if (deviceId != null && !deviceId.isBlank()) {
            return deviceId.trim();
        }
        if (device != null && device.deviceId() != null && !device.deviceId().isBlank()) {
            return device.deviceId().trim();
        }
        return null;
    }
}
