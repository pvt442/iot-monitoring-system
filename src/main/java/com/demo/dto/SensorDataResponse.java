package com.demo.dto;

import java.util.List;

public record SensorDataResponse(
        Integer dataId,
        /** Mã thiết bị (dạng phẳng, tiện cho Frontend) */
        String deviceId,
        /** Thông tin thiết bị (dạng lồng, tương thích cấu trúc cũ device.deviceId) */
        DeviceInfo device,
        double temperature,
        double humidity,
        String timestamp,
        /** NORMAL | WARNING | CRITICAL - đánh giá theo ngưỡng cấu hình hiện hành */
        String alertStatus,
        List<AlertItem> alerts) {
}
