package com.demo.dto;

/** Thông tin thiết bị gọn, lồng trong bản ghi cảm biến (giữ tương thích cấu trúc device.deviceId cũ). */
public record DeviceInfo(String deviceId, String name, String location, String status) {
}
