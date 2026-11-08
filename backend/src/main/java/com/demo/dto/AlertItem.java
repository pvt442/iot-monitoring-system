package com.demo.dto;

/**
 * Một điều kiện cảnh báo của một lần đo.
 * metric: TEMPERATURE | HUMIDITY; level: WARNING (sắp vượt) | CRITICAL (vượt ngưỡng).
 */
public record AlertItem(String metric, String level, double value, double threshold, String message) {
}
