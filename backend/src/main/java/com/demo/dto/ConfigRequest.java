package com.demo.dto;

/** Body của PUT /api/config. Trường nào null thì giữ nguyên giá trị hiện tại. */
public record ConfigRequest(
        Double temperatureThreshold,
        Double humidityThreshold,
        Double temperatureWarningMargin,
        Double humidityWarningMargin,
        Integer offlineTimeoutSeconds) {
}
