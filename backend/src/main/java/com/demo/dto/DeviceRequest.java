package com.demo.dto;

/** Body của POST /api/devices. Chỉ deviceId là bắt buộc. */
public record DeviceRequest(String deviceId, String name, String location, String status) {
}
