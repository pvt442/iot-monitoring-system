package com.demo.dto;

import java.util.List;

public record OverviewResponse(
        /** Giờ hiện tại của Server - Frontend dùng để tính "x giây trước" không phụ thuộc đồng hồ máy khách */
        String serverTime,
        Summary summary,
        ConfigResponse thresholds,
        List<DeviceOverview> devices) {

    /**
     * total: tổng thiết bị; online/offline: theo timeout; alerting: số thiết bị ĐANG online có số đo mới nhất vượt ngưỡng;
     * warning: số thiết bị online ở vùng sắp vượt ngưỡng.
     */
    public record Summary(int totalDevices, int online, int offline, int alerting, int warning) {
    }
}
