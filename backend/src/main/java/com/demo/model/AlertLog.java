package com.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Nhật ký các lần thiết bị vượt ngưỡng, do Server lưu bền vững (bảng alert_logs). */
@Entity
@Table(name = "alert_logs",
        indexes = @Index(name = "idx_alert_device_time", columnList = "device_id, created_at"))
public class AlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /** Mã bản ghi cảm biến gây ra cảnh báo (sensor_data.data_id). */
    @Column(name = "data_id")
    private Integer dataId;

    /** TEMPERATURE hoặc HUMIDITY */
    @Column(length = 20, nullable = false)
    private String metric;

    /** CRITICAL (vượt ngưỡng) */
    @Column(name = "alert_level", length = 20, nullable = false)
    private String level;

    @Column(name = "actual_value")
    private Float actualValue;

    @Column(name = "threshold_value")
    private Float thresholdValue;

    @Column(length = 255)
    private String message;

    @Column(name = "created_at", columnDefinition = "DATETIME(3)")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public Integer getDataId() { return dataId; }
    public void setDataId(Integer dataId) { this.dataId = dataId; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Float getActualValue() { return actualValue; }
    public void setActualValue(Float actualValue) { this.actualValue = actualValue; }

    public Float getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Float thresholdValue) { this.thresholdValue = thresholdValue; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
