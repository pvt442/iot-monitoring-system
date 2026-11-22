package com.demo.dto;

public record ConfigResponse(
        double temperatureThreshold,
        double humidityThreshold,
        double temperatureWarningMargin,
        double humidityWarningMargin,
        int offlineTimeoutSeconds,
        String updatedAt) {
}
