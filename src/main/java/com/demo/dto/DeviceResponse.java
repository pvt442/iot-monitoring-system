package com.demo.dto;

public record DeviceResponse(
        String deviceId,
        String name,
        String location,
        /** Trạng thái quản lý: ACTIVE / INACTIVE */
        String status,
        /** Lần cuối Server nhận dữ liệu của thiết bị (có thể null nếu chưa từng gửi) */
        String lastSeen,
        /** true nếu nhận dữ liệu trong khoảng offlineTimeoutSeconds gần nhất */
        boolean online,
        /** ONLINE / OFFLINE */
        String connection) {
}
