package com.demo.dto;

public record AlertLogResponse(
        Long id,
        String deviceId,
        String deviceName,
        Integer dataId,
        String metric,
        String level,
        double value,
        double threshold,
        String message,
        String createdAt,
        /** Luôn là BACKEND: cảnh báo do Server phát hiện và lưu vào MySQL */
        String source) {
}
