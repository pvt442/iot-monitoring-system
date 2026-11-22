package com.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cấu hình ngưỡng cảnh báo dùng chung toàn hệ thống. Bảng alert_config luôn chỉ có một dòng (id = 1). */
@Entity
@Table(name = "alert_config")
public class AlertConfig {

    public static final int SINGLETON_ID = 1;
    public static final float DEFAULT_TEMP_THRESHOLD = 35f;
    public static final float DEFAULT_HUM_THRESHOLD = 80f;
    public static final float DEFAULT_TEMP_MARGIN = 3f;
    public static final float DEFAULT_HUM_MARGIN = 5f;
    public static final int DEFAULT_OFFLINE_TIMEOUT = 15;

    @Id
    private Integer id = SINGLETON_ID;

    /** Cảnh báo khi nhiệt độ > ngưỡng (°C) */
    @Column(name = "temperature_threshold", nullable = false)
    private Float temperatureThreshold = DEFAULT_TEMP_THRESHOLD;

    /** Cảnh báo khi độ ẩm > ngưỡng (%) */
    @Column(name = "humidity_threshold", nullable = false)
    private Float humidityThreshold = DEFAULT_HUM_THRESHOLD;

    /** Vùng "sắp vượt ngưỡng": từ (ngưỡng - biên) đến ngưỡng */
    @Column(name = "temperature_warning_margin", nullable = false)
    private Float temperatureWarningMargin = DEFAULT_TEMP_MARGIN;

    @Column(name = "humidity_warning_margin", nullable = false)
    private Float humidityWarningMargin = DEFAULT_HUM_MARGIN;

    /** Quá số giây này không nhận dữ liệu mới thì thiết bị bị coi là Offline */
    @Column(name = "offline_timeout_seconds", nullable = false)
    private Integer offlineTimeoutSeconds = DEFAULT_OFFLINE_TIMEOUT;

    @Column(name = "updated_at", columnDefinition = "DATETIME(3)")
    private LocalDateTime updatedAt;

    public static AlertConfig defaults() {
        AlertConfig c = new AlertConfig();
        c.updatedAt = LocalDateTime.now();
        return c;
    }

    public void resetToDefaults() {
        this.temperatureThreshold = DEFAULT_TEMP_THRESHOLD;
        this.humidityThreshold = DEFAULT_HUM_THRESHOLD;
        this.temperatureWarningMargin = DEFAULT_TEMP_MARGIN;
        this.humidityWarningMargin = DEFAULT_HUM_MARGIN;
        this.offlineTimeoutSeconds = DEFAULT_OFFLINE_TIMEOUT;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Float getTemperatureThreshold() { return temperatureThreshold; }
    public void setTemperatureThreshold(Float v) { this.temperatureThreshold = v; }

    public Float getHumidityThreshold() { return humidityThreshold; }
    public void setHumidityThreshold(Float v) { this.humidityThreshold = v; }

    public Float getTemperatureWarningMargin() { return temperatureWarningMargin; }
    public void setTemperatureWarningMargin(Float v) { this.temperatureWarningMargin = v; }

    public Float getHumidityWarningMargin() { return humidityWarningMargin; }
    public void setHumidityWarningMargin(Float v) { this.humidityWarningMargin = v; }

    public Integer getOfflineTimeoutSeconds() { return offlineTimeoutSeconds; }
    public void setOfflineTimeoutSeconds(Integer v) { this.offlineTimeoutSeconds = v; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
