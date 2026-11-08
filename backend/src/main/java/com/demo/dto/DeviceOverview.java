package com.demo.dto;

/** Một thiết bị kèm số đo mới nhất - phục vụ Dashboard chỉ cần 1 lần gọi API. */
public record DeviceOverview(
        String deviceId,
        String name,
        String location,
        String status,
        String lastSeen,
        boolean online,
        String connection,
        /** Đánh giá của số đo mới nhất: NORMAL | WARNING | CRITICAL (NORMAL nếu chưa có dữ liệu) */
        String alertStatus,
        /** Số đo mới nhất, null nếu thiết bị chưa gửi dữ liệu */
        SensorDataResponse latest) {
}
